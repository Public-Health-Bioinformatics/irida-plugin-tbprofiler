package org.publichealthbioinformatics.irida.plugin.tbprofiler;

import ca.corefacility.bioinformatics.irida.exceptions.IridaWorkflowNotFoundException;
import ca.corefacility.bioinformatics.irida.exceptions.PostProcessingException;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.sample.metadata.MetadataEntry;
import ca.corefacility.bioinformatics.irida.model.sample.metadata.PipelineProvidedMetadataEntry;
import ca.corefacility.bioinformatics.irida.model.workflow.IridaWorkflow;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.AnalysisOutputFile;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.type.AnalysisType;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.AnalysisSubmission;
import ca.corefacility.bioinformatics.irida.pipeline.results.updater.AnalysisSampleUpdater;
import ca.corefacility.bioinformatics.irida.service.sample.MetadataTemplateService;
import ca.corefacility.bioinformatics.irida.service.sample.SampleService;
import ca.corefacility.bioinformatics.irida.service.workflow.IridaWorkflowsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * This implements a class used to perform post-processing on the analysis
 * pipeline results to extract information to write into the IRIDA metadata
 * tables. Please see
 * <https://github.com/phac-nml/irida/blob/development/src/main/java/ca/corefacility/bioinformatics/irida/pipeline/results/AnalysisSampleUpdater.java>
 * or the README.md file in this project for more details.
 */
public class TBProfilerPluginUpdater implements AnalysisSampleUpdater {

	private final MetadataTemplateService metadataTemplateService;
	private final SampleService sampleService;
	private final IridaWorkflowsService iridaWorkflowsService;

	/**
	 * Builds a new {@link TBProfilerPluginUpdater} with the given services.
	 * 
	 * @param metadataTemplateService The metadata template service.
	 * @param sampleService           The sample service.
	 * @param iridaWorkflowsService   The irida workflows service.
	 */
	public TBProfilerPluginUpdater(MetadataTemplateService metadataTemplateService, SampleService sampleService,
								IridaWorkflowsService iridaWorkflowsService) {
		this.metadataTemplateService = metadataTemplateService;
		this.sampleService = sampleService;
		this.iridaWorkflowsService = iridaWorkflowsService;
	}

	/**
	 * Code to perform the actual update of the {@link Sample}s passed in the
	 * collection.
	 * 
	 * @param samples  A collection of {@link Sample}s that were passed to this
	 *                 pipeline.
	 * @param analysis The {@link AnalysisSubmission} object corresponding to this
	 *                 analysis pipeline.
	 */
	@Override
	public void update(Collection<Sample> samples, AnalysisSubmission analysis) throws PostProcessingException {
		if (samples == null) {
			throw new IllegalArgumentException("samples is null");
		} else if (analysis == null) {
			throw new IllegalArgumentException("analysis is null");
		} else if (samples.size() != 1) {
			// In this particular pipeline, only one sample should be run at a time, so I
			// verify that the collection of samples I get has only 1 sample
			throw new IllegalArgumentException(
					"samples size=" + samples.size() + " is not 1 for analysisSubmission=" + analysis.getId());
		}

		// extract the 1 and only sample (if more than 1, would have thrown an exception above)
		final Sample sample = samples.iterator().next();

		// extracts paths to the analysis result files
		AnalysisOutputFile tbprofilerResultsAnalysisOutputFile = analysis.getAnalysis().getAnalysisOutputFile("tbprofiler_results");
		Path tbProfilerResultsFilePath = tbprofilerResultsAnalysisOutputFile.getFile();

		List<String> drugList= Arrays.asList(
				"rifampicin",
				"isoniazid",
				"ethambutol",
				"pyrazinamide",
				"streptomycin",
				"fluoroquinolones",
				"moxifloxacin",
				"ofloxacin",
				"levofloxacin",
				"ciprofloxacin",
				"aminoglycosides",
				"amikacin",
				"capreomycin",
				"kanamycin",
				"cycloserine",
				"ethionamide",
				"clofazimine",
				"para-aminosalicylic_acid",
				"delamanid",
				"bedaquiline",
				"linezolid"
		);

		try {
			Map<String, MetadataEntry> metadataEntries = new HashMap<>();

			// get information about the workflow (e.g., version and name)
			IridaWorkflow iridaWorkflow = iridaWorkflowsService.getIridaWorkflow(analysis.getWorkflowId());
			String workflowVersion = iridaWorkflow.getWorkflowDescription().getVersion();
			String workflowName = iridaWorkflow.getWorkflowDescription().getName();

			// gets information from the "tbprofiler_results.json" output file and constructs metadata
			// objects
			Map<String, String> tbProfilerResults = parseTbProfilerResultsFile(tbProfilerResultsFilePath);

			String percentReadsMapped = tbProfilerResults.get("pct_reads_mapped");
			String resistanceType = tbProfilerResults.get("drtype");
			String mainLineage = tbProfilerResults.get("main_lin").replaceFirst("^lineage", "");
			String subLineage = tbProfilerResults.get("sublin").replaceFirst("^lineage", "");
			String tbprofilerVersion = tbProfilerResults.get("tbprofiler_version");
			String dbName = tbProfilerResults.get("db_name");
			String dbCommit = tbProfilerResults.get("db_commit");
			String dbDate = tbProfilerResults.get("db_date");

			String key = "";

			// add
			PipelineProvidedMetadataEntry tbProfilerPercentReadsMappedEntry = new PipelineProvidedMetadataEntry(percentReadsMapped, "xs:float", analysis);
			key = workflowName + "/percent_reads_mapped";
			metadataEntries.put(key, tbProfilerPercentReadsMappedEntry);

			PipelineProvidedMetadataEntry tbProfilerDrTypeEntry = new PipelineProvidedMetadataEntry(resistanceType, "xs:string", analysis);
			key = workflowName + "/resistance_type";
			metadataEntries.put(key, tbProfilerDrTypeEntry);

			PipelineProvidedMetadataEntry tbProfilerMainLineageEntry = new PipelineProvidedMetadataEntry(mainLineage, "xs:string", analysis);
			key = workflowName + "/main_lineage";
			metadataEntries.put(key, tbProfilerMainLineageEntry);

			PipelineProvidedMetadataEntry tbProfilerSubLineageEntry = new PipelineProvidedMetadataEntry(subLineage, "xs:string", analysis);
			key = workflowName + "/sub_lineage";
			metadataEntries.put(key, tbProfilerSubLineageEntry);

			PipelineProvidedMetadataEntry tbProfilerTbprofilerVersionEntry = new PipelineProvidedMetadataEntry(tbprofilerVersion, "xs:string", analysis);
			key = workflowName + "/tbprofiler_version";
			metadataEntries.put(key, tbProfilerTbprofilerVersionEntry);

			PipelineProvidedMetadataEntry tbProfilerDbNameEntry = new PipelineProvidedMetadataEntry(dbName, "xs:string", analysis);
			key = workflowName + "/db_name";
			metadataEntries.put(key, tbProfilerDbNameEntry);

			PipelineProvidedMetadataEntry tbProfilerDbCommitEntry = new PipelineProvidedMetadataEntry(dbCommit, "xs:string", analysis);
			key = workflowName + "/db_commit";
			metadataEntries.put(key, tbProfilerDbCommitEntry);

			PipelineProvidedMetadataEntry tbProfilerDbDateEntry = new PipelineProvidedMetadataEntry(dbDate, "xs:string", analysis);
			key = workflowName + "/db_date";
			metadataEntries.put(key, tbProfilerDbDateEntry);

			for (String drug : drugList) {
				String drugResistance = tbProfilerResults.get(drug + "_resistance");
				PipelineProvidedMetadataEntry tbProfilerDrugResistanceEntry = new PipelineProvidedMetadataEntry(drugResistance, "xs:string", analysis);
				key = workflowName + "/" + drug + "_resistance";
				metadataEntries.put(key, tbProfilerDrugResistanceEntry);
			}
			for (String drug : drugList) {
				String drugResistanceMutations = tbProfilerResults.get(drug + "_mutations");
				PipelineProvidedMetadataEntry tbProfilerDrugMutationsEntry = new PipelineProvidedMetadataEntry(drugResistanceMutations, "xs:string", analysis);
				key = workflowName + "/" + drug + "_mutations";
				metadataEntries.put(key, tbProfilerDrugMutationsEntry);
			}
			//convert the string/entry Map to a Set of MetadataEntry
			Set<MetadataEntry> metadataSet = metadataTemplateService.convertMetadataStringsToSet(metadataEntries);

			// merges with existing sample metadata and does an update of the sample metadata.
			sampleService.mergeSampleMetadata(sample,metadataSet);

		} catch (IOException e) {
			throw new PostProcessingException("Error parsing gene detection status file", e);
		} catch (IridaWorkflowNotFoundException e) {
			throw new PostProcessingException("Could not find workflow for id=" + analysis.getWorkflowId(), e);
		}
	}


	/**
	 * Parses out values from the hash file into a {@link List<Map>} linking 'gene_name' to 'detection_status'.
	 * 
	 * @param tbProfilerResultsFilePath The {@link Path} to the file containing the hash values from
	 *                 the pipeline. This file should contain contents like:
	 * 
	 *                 <pre>
	 * {
	 *     "qc": {
	 *         "pct_reads_mapped": 99.08,
	 *         "num_reads_mapped": 1837357
	 *     },
	 *     "region_coverage": {},
	 *     "lineage": [
	 *     ...
	 *  }
	 *                 </pre>
	 * 
	 * @return An {@link List<Map>} linking 'geneName' to 'detectionStatus'.
	 * @throws IOException             If there was an error reading the file.
	 * @throws PostProcessingException If there was an error parsing the file.
	 */
	@VisibleForTesting
	Map<String, String> parseTbProfilerResultsFile(Path tbProfilerResultsFilePath) throws IOException, PostProcessingException {
		Map<String, String> tbProfilerReport = new HashMap<>();

		ObjectMapper mapper = new ObjectMapper();

		List<String> drugList= Arrays.asList(
				"rifampicin",
				"isoniazid",
				"ethambutol",
				"pyrazinamide",
				"streptomycin",
				"fluoroquinolones",
				"moxifloxacin",
				"ofloxacin",
				"levofloxacin",
				"ciprofloxacin",
				"aminoglycosides",
				"amikacin",
				"capreomycin",
				"kanamycin",
				"cycloserine",
				"ethionamide",
				"clofazimine",
				"para-aminosalicylic_acid",
				"delamanid",
				"bedaquiline",
				"linezolid"
		);

		JsonNode root = mapper.readTree(tbProfilerResultsFilePath.toFile());
		tbProfilerReport.put("pct_reads_mapped", root.get("qc").get("pct_reads_mapped").asText());
		tbProfilerReport.put("drtype", root.get("drtype").asText());
		tbProfilerReport.put("main_lin", root.get("main_lin").asText());
		tbProfilerReport.put("sublin", root.get("sublin").asText());
		tbProfilerReport.put("tbprofiler_version", root.get("tbprofiler_version").asText());
		tbProfilerReport.put("db_name", root.get("db_version").get("name").asText());
		tbProfilerReport.put("db_commit", root.get("db_version").get("commit").asText());
		tbProfilerReport.put("db_date", root.get("db_version").get("Date").asText());

		for (String drug : drugList) {
			tbProfilerReport.put(drug + "_resistance", "Sensitive");
			tbProfilerReport.put(drug + "_mutations", "None");
		}

		Iterator<JsonNode> drVariants =  root.get("dr_variants").elements();
		while (drVariants.hasNext()) {
			JsonNode drVariant = drVariants.next();
			Iterator<JsonNode> variantDrugs = drVariant.get("drugs").elements();
			while (variantDrugs.hasNext()) {
				JsonNode variantDrug = variantDrugs.next();
				String drugName = variantDrug.get("drug").asText();
				String drugConfers = variantDrug.get("confers").asText();
				if (drugConfers.equals("resistance")) {
					tbProfilerReport.put(drugName + "_resistance", "Resistant");
					String geneName = drVariant.get("gene").asText();
					String change = drVariant.get("change").asText();
					double freq = drVariant.get("freq").asDouble();
					String freqStr = String.format("%.2f", freq);
					String existingMutations = tbProfilerReport.get(drugName + "_mutations");
					if (existingMutations.equals("None")) {
						String mutations = String.join(" ", geneName, change, "(" + freqStr + ")");
						tbProfilerReport.put(drugName + "_mutations", mutations);
					} else {
						String newMutation = String.join(" ", geneName, change, "(" + freqStr + ")");
						String mutations = String.join("; ", existingMutations, newMutation);
						tbProfilerReport.put(drugName + "_mutations", mutations);
					}
				}
			}
		}

		return tbProfilerReport;
	}

	/**
	 * The {@link AnalysisType} this {@link AnalysisSampleUpdater} corresponds to.
	 * 
	 * @return The {@link AnalysisType} this {@link AnalysisSampleUpdater}
	 *         corresponds to.
	 */
	@Override
	public AnalysisType getAnalysisType() {
		return TBProfilerPlugin.TBPROFILER;
	}
}
