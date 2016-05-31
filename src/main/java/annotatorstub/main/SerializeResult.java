package annotatorstub.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;

import annotatorstub.annotator.PBoHModelAnnotator;
import annotatorstub.annotator.PBoHModelAnnotatorWithCorrection;
import annotatorstub.utils.Utils;
import it.unipi.di.acube.batframework.cache.BenchmarkCache;
import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.datasetPlugins.YahooWebscopeL24Dataset;
import it.unipi.di.acube.batframework.metrics.Metrics;
import it.unipi.di.acube.batframework.metrics.MetricsResultSet;
import it.unipi.di.acube.batframework.metrics.StrongAnnotationMatch;
import it.unipi.di.acube.batframework.metrics.StrongTagMatch;
import it.unipi.di.acube.batframework.problems.A2WDataset;
import it.unipi.di.acube.batframework.utils.DumpData;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

public class SerializeResult {
	public static void main(String[] args) throws Exception {
		WikipediaApiInterface wikiApi = WikipediaApiInterface.api();
		String dsPath = "out-domain-dataset.xml";
		String groupName = "DataMiners";
		A2WDataset ds = new YahooWebscopeL24Dataset(dsPath);
//		FakeAnnotator ann = new FakeAnnotator();
//		newAnnotator ann = new newAnnotator();
		PBoHModelAnnotatorWithCorrection ann = new PBoHModelAnnotatorWithCorrection();
//		PBoHModelAnnotatorWith ann = new PBoHModelAnnotator();		
		Utils.serializeResult(ann, ds, new File("annotation-" + groupName + "-simple" + ".bin"));
	}
}