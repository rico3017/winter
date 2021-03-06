/** 
 *
 * Copyright (C) 2015 Data and Web Science Group, University of Mannheim, Germany (code@dwslab.de)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.uni_mannheim.informatik.dws.winter.usecase.restaurants;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.uni_mannheim.informatik.dws.winter.matching.MatchingEngine;
import de.uni_mannheim.informatik.dws.winter.matching.MatchingEvaluator;
import de.uni_mannheim.informatik.dws.winter.matching.algorithms.RuleLearner;
import de.uni_mannheim.informatik.dws.winter.matching.blockers.StandardRecordBlocker;
import de.uni_mannheim.informatik.dws.winter.matching.blockers.generators.BlockingKeyGenerator;
import de.uni_mannheim.informatik.dws.winter.matching.blockers.generators.RecordBlockingKeyGenerator;
import de.uni_mannheim.informatik.dws.winter.matching.rules.WekaMatchingRule;
import de.uni_mannheim.informatik.dws.winter.model.Correspondence;
import de.uni_mannheim.informatik.dws.winter.model.DataSet;
import de.uni_mannheim.informatik.dws.winter.model.HashedDataSet;
import de.uni_mannheim.informatik.dws.winter.model.Matchable;
import de.uni_mannheim.informatik.dws.winter.model.MatchingGoldStandard;
import de.uni_mannheim.informatik.dws.winter.model.Pair;
import de.uni_mannheim.informatik.dws.winter.model.Performance;
import de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Attribute;
import de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record;
import de.uni_mannheim.informatik.dws.winter.model.defaultmodel.XMLRecordReader;
import de.uni_mannheim.informatik.dws.winter.model.defaultmodel.comparators.RecordComparatorEqual;
import de.uni_mannheim.informatik.dws.winter.model.defaultmodel.comparators.RecordComparatorJaccard;
import de.uni_mannheim.informatik.dws.winter.model.defaultmodel.comparators.RecordComparatorLevenshtein;
import de.uni_mannheim.informatik.dws.winter.model.io.CSVCorrespondenceFormatter;
import de.uni_mannheim.informatik.dws.winter.processing.DataIterator;
import de.uni_mannheim.informatik.dws.winter.processing.Processable;
import de.uni_mannheim.informatik.dws.winter.usecase.restaurants.model.Restaurant;
import de.uni_mannheim.informatik.dws.winter.utils.WinterLogManager;


/**
 * Class containing the standard setup to perform a identity resolution task by using learning matching rules,
 * reading input data from the Restaurants use case.
 * 
 * @author Alexander Brinkmann (albrinkm@mail.uni-mannheim.de)
 * 
 */
public class Restaurants_IdentityResolutionLearningMatchingRule {

	/*
	 * Logging Options:
	 * 		default: 	level INFO	- console
	 * 		trace:		level TRACE     - console
	 * 		infoFile:	level INFO	- console/file
	 * 		traceFile:	level TRACE	- console/file
	 *  
	 * To set the log level to trace and write the log to winter.log and console, 
	 * activate the "traceFile" logger as follows:
	 *     private static final Logger logger = WinterLogManager.activateLogger("traceFile");
	 *
	 */

	private static final Logger logger = WinterLogManager.activateLogger("default");
	
	public static void main(String[] args) throws Exception {
		// loading data
		Map<String, Attribute> nodeMapping = new HashMap<>();
		nodeMapping.put("Name", Restaurant.NAME);
		nodeMapping.put("Address", Restaurant.ADDRESS);
		nodeMapping.put("City", Restaurant.CITY);
		//nodeMapping.put("Phone", Restaurant.PHONE);
		nodeMapping.put("Style", Restaurant.STYLE);

		DataSet<Record, Attribute> dataZagats = new HashedDataSet<>();
		new XMLRecordReader("id", nodeMapping).loadFromXML(new File("usecase/restaurant/input/zagats.xml"),
				"/restaurants/restaurant", dataZagats);
		HashedDataSet<Record, Attribute> dataFodors = new HashedDataSet<>();
		new XMLRecordReader("id", nodeMapping).loadFromXML(new File("usecase/restaurant/input/fodors.xml"),
				"/restaurants/restaurant", dataFodors);

		// load the gold standard (training set)
		MatchingGoldStandard gsTraining = new MatchingGoldStandard();
		gsTraining.loadFromCSVFile(new File("usecase/restaurant/goldstandard/gs_restaurant_training.csv"));

		// create a matching rule + provide classifier, options + Feature
		String options[] = new String[1];
		options[0] = ""; // unpruned tree
		String tree = "J48"; // new instance of tree
		WekaMatchingRule<Record, Attribute> matchingRule = new WekaMatchingRule<>(0.8, tree, options);
		
		// Collect debug results
		matchingRule.activateDebugReport("usecase/restaurant/output/debugResultsWekaMatchingRule.csv", 1000, gsTraining);
		
		// add comparators - Name
		matchingRule.addComparator(new RecordComparatorLevenshtein(Restaurant.NAME, Restaurant.NAME));
		matchingRule.addComparator(new RecordComparatorEqual(Restaurant.NAME, Restaurant.NAME));
		RecordComparatorEqual nameLowerCaseEqual = new RecordComparatorEqual(Restaurant.NAME, Restaurant.NAME);
		nameLowerCaseEqual.setLowerCase(true);
		matchingRule.addComparator(nameLowerCaseEqual);
		matchingRule.addComparator(new RecordComparatorJaccard(Restaurant.NAME, Restaurant.NAME, 0.3, true));
		RecordComparatorLevenshtein nameLowerCaseLevenshtein = new RecordComparatorLevenshtein(Restaurant.NAME, Restaurant.NAME);
		nameLowerCaseLevenshtein.setLowerCase(true);
		matchingRule.addComparator(nameLowerCaseLevenshtein);
		RecordComparatorJaccard lowerCaseName = new RecordComparatorJaccard(Restaurant.NAME, Restaurant.NAME, 0.3, true);
		lowerCaseName.setLowerCase(true);
		matchingRule.addComparator(lowerCaseName);
		
		
		matchingRule.addComparator(new RecordComparatorLevenshtein(Restaurant.ADDRESS, Restaurant.ADDRESS));
		matchingRule.addComparator(new RecordComparatorEqual(Restaurant.ADDRESS, Restaurant.ADDRESS));
		RecordComparatorEqual addressLowerCaseEqual = new RecordComparatorEqual(Restaurant.ADDRESS, Restaurant.ADDRESS);
		addressLowerCaseEqual.setLowerCase(true);
		matchingRule.addComparator(addressLowerCaseEqual);
		matchingRule.addComparator(new RecordComparatorJaccard(Restaurant.ADDRESS, Restaurant.ADDRESS, 0.3, true));
		RecordComparatorLevenshtein addressLowerCaseLevenshtein = new RecordComparatorLevenshtein(Restaurant.ADDRESS, Restaurant.ADDRESS);
		addressLowerCaseLevenshtein.setLowerCase(true);
		matchingRule.addComparator(addressLowerCaseLevenshtein);
		RecordComparatorJaccard lowerCaseAddress = new RecordComparatorJaccard(Restaurant.ADDRESS, Restaurant.ADDRESS, 0.3, true);
		lowerCaseAddress.setLowerCase(true);
		matchingRule.addComparator(lowerCaseAddress);

		
		matchingRule.addComparator(new RecordComparatorLevenshtein(Restaurant.STYLE, Restaurant.STYLE));
		matchingRule.addComparator(new RecordComparatorEqual(Restaurant.STYLE, Restaurant.STYLE));
		RecordComparatorEqual styleLowerCaseEqual = new RecordComparatorEqual(Restaurant.STYLE, Restaurant.STYLE);
		styleLowerCaseEqual.setLowerCase(true);
		matchingRule.addComparator(styleLowerCaseEqual);
		matchingRule.addComparator(new RecordComparatorJaccard(Restaurant.STYLE, Restaurant.STYLE, 0.3, true));
		RecordComparatorLevenshtein styleLowerCaseLevenshtein = new RecordComparatorLevenshtein(Restaurant.STYLE, Restaurant.STYLE);
		styleLowerCaseLevenshtein.setLowerCase(true);
		matchingRule.addComparator(styleLowerCaseLevenshtein);
		RecordComparatorJaccard lowerCaseStyle = new RecordComparatorJaccard(Restaurant.STYLE, Restaurant.STYLE, 0.3, true);
		lowerCaseStyle.setLowerCase(true);
		matchingRule.addComparator(lowerCaseStyle);
		

		// create a blocker (blocking strategy)
		StandardRecordBlocker<Record, Attribute> blocker = new StandardRecordBlocker<>(
				new RecordBlockingKeyGenerator<Record, Attribute>() {

					/**
					 * {@link BlockingKeyGenerator} for {@link Restaurant}s,
					 * which generates a blocking key based on the city in which
					 * a restaurant is located. E.g. Los Angeles
					 * 
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void generateBlockingKeys(Record record,
							Processable<Correspondence<Attribute, Matchable>> correspondences,
							DataIterator<Pair<String, Record>> resultCollector) {
						resultCollector.next(new Pair<>((record.getValue(Restaurant.CITY)), record));

					}
				});
		
		// Write Debug Results to file
		blocker.collectBlockSizeData("usecase/restaurant/output/debugResultsBlocking.csv", 100);
		
		// learning Matching rule
		RuleLearner<Record, Attribute> learner = new RuleLearner<>();
		learner.learnMatchingRule(dataFodors, dataZagats, null, matchingRule, gsTraining);

		// Store Matching Rule
		matchingRule.exportModel(new File("usecase/restaurant/matchingRule/restaurantMatchingModel.model"));
		
		// Initialize Matching Engine
		MatchingEngine<Record, Attribute> engine = new MatchingEngine<>();
		// Execute the matching
		Processable<Correspondence<Record, Attribute>> correspondences = engine.runIdentityResolution(dataFodors,
				dataZagats, null, matchingRule, blocker);

		// write the correspondences to the output file
		new CSVCorrespondenceFormatter().writeCSV(new File("usecase/restaurant/output/restaurant_correspondences.csv"),
				correspondences);

		// load the gold standard (test set)
		MatchingGoldStandard gsTest = new MatchingGoldStandard();
		gsTest.loadFromCSVFile(new File("usecase/restaurant/goldstandard/gs_restaurant_test.csv"));
		
		// evaluate your result
		MatchingEvaluator<Record, Attribute> evaluator = new MatchingEvaluator<Record, Attribute>();
		Performance perfTest = evaluator.evaluateMatching(correspondences.get(), gsTest);
		
		//evaluate learned classifier
		logger.info(matchingRule.getClassifier().toString());

		// print the evaluation result
		logger.info("Fodors <-> Zagats");
		logger.info(String.format("Precision: %.4f", perfTest.getPrecision()));
		logger.info(String.format("Recall: %.4f", perfTest.getRecall()));
		logger.info(String.format("F1: %.4f", perfTest.getF1()));

	}
}
