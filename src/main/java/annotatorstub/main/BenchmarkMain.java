package annotatorstub.main;

import it.unipi.di.acube.batframework.cache.BenchmarkCache;
import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.data.Mention;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder;
import it.unipi.di.acube.batframework.metrics.Metrics;
import it.unipi.di.acube.batframework.metrics.MetricsResultSet;
import it.unipi.di.acube.batframework.metrics.StrongAnnotationMatch;
import it.unipi.di.acube.batframework.metrics.StrongTagMatch;
import it.unipi.di.acube.batframework.problems.A2WDataset;
import it.unipi.di.acube.batframework.utils.DumpData;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import annotatorstub.annotator.FakeAnnotator;
import annotatorstub.annotator.newAnnotator;

public class BenchmarkMain {
	public static void main(String[] args) throws Exception {
		WikipediaApiInterface wikiApi = WikipediaApiInterface.api();
		A2WDataset ds = DatasetBuilder.getGerdaqDevel();
//		FakeAnnotator ann = new FakeAnnotator();
		newAnnotator ann = new newAnnotator();

		List<HashSet<Tag>> resTag = BenchmarkCache.doC2WTags(ann, ds);
		List<HashSet<Annotation>> resAnn = BenchmarkCache.doA2WAnnotations(ann, ds);
		DumpData.dumpCompareList(ds.getTextInstanceList(), ds.getA2WGoldStandardList(), resAnn, wikiApi);

		Metrics<Tag> metricsTag = new Metrics<>();
		MetricsResultSet C2WRes = metricsTag.getResult(resTag, ds.getC2WGoldStandardList(), new StrongTagMatch(wikiApi));
		printMetricsResultSet("C2W", C2WRes, ann.getName());

		Metrics<Annotation> metricsAnn = new Metrics<>();
		MetricsResultSet rsA2W = metricsAnn.getResult(resAnn, ds.getA2WGoldStandardList(), new StrongAnnotationMatch(wikiApi));
		printMetricsResultSet("A2W-SAM", rsA2W, ann.getName());
		
		wikiApi.flush();
		
		
//		EchoQuery();
		
	}

	private static void printMetricsResultSet(String exp, MetricsResultSet rs, String annName){
		System.out
		.format(Locale.ENGLISH,
				"%s\t mac-P/R/F1: %.3f\t%.3f\t%.3f TP/FP/FN: %d\t%d\t%d mic-P/R/F1: %.3f\t%.3f\t%.3f\t%s%n", exp,
				rs.getMacroPrecision(), rs.getMacroRecall(),
				rs.getMacroF1(), rs.getGlobalTp(),
				rs.getGlobalFp(), rs.getGlobalFn(),rs.getMicroPrecision(), rs.getMicroRecall(),
				rs.getMicroF1(), annName);
	}
	
	public static void EchoQuery(){
		System.out.println("---------------------------------------query");
		A2WDataset ds = DatasetBuilder.getGerdaqDevel();
		List<HashSet<Annotation>> truthAnno = ds.getA2WGoldStandardList();
		List<HashSet<Mention>>truthMen = ds.getMentionsInstanceList();
		System.out.println("Query size: " + ds.getTextInstanceList().size());
		for(String str : ds.getTextInstanceList()){
			System.out.println(str);
		}
		

	}

}
