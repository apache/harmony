/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import junit.extensions.TestDecorator;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Decorator for JUnit test suites that we want to run with certain test cases
 * excluded. The exclusions are captured in a separate XML file.
 */
public class SomeTests extends TestSetup {

	private static final String EXCLUDES_SCHEMA_URI_PROP = "excludes.schema.uri";

	private static final String EXCLUDES_FILE_URI_PROP = "excludes.file.uri";

	private static final String JAXP_SCHEMA_SOURCE_ATTR_NAME = "http://java.sun.com/xml/jaxp/properties/schemaSource";

	private static final String JAXP_SCHEMA_LANGUAGE_ATTR_VAL = "http://www.w3.org/2001/XMLSchema";

	private static final String JAXP_SCHEMA_LANGUAGE_ATTR_NAME = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	private static final String DOCUMENT_BUILDER_FACTORY_VAL = "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl";

	private static final String DOCUMENT_BUILDER_FACTORY_PROP = "javax.xml.parsers.DocumentBuilderFactory";

	private Node excludesDoc;

	private final List<TestCase> runCases = new ArrayList<TestCase>();

	private String excludesListURI;

	private String excludesSchemaURI;

	int originalTestCaseCount;

	private boolean docIsValid = false;

	/**
	 * @param test
	 * @param excludesListURI
	 * @param excludesSchemaURI
	 */
	public SomeTests(Test test) {
		super(test);
		initExcludes();

		// Drill down into each test case contained in the supplied Test
		// and, for each one *not* excluded, add the corresponding TestCase
		// to an ordered collection. The number of TestCase objects in the
		// collection is the number of test cases that will be run.
		seekTests(this.getTest());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.extensions.TestDecorator#basicRun(junit.framework.TestResult)
	 */
	@Override
    public void basicRun(TestResult result) {
		// The 'basic run' of this decorator is to enumerate through all
		// of the tests not excluded and run them...
		Iterator<TestCase> allIncluded = runCases.iterator();
		while (allIncluded.hasNext()) {
			TestCase test = allIncluded.next();
			test.run(result);
		}// end while
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.extensions.TestSetup#setUp()
	 */
	@Override
    protected void setUp() throws Exception {
		if (docIsValid) {
			System.out.println("\nExcludes list document " + excludesListURI
					+ " is valid");
		}
		System.out.println("\nRunning " + this.countTestCases()
				+ " out of a total of " + this.originalTestCaseCount
				+ " possible test cases\n");
	}

	@Override
    protected void tearDown() throws Exception {
		System.out.println("\nRan " + this.countTestCases()
				+ " out of a total of " + this.originalTestCaseCount
				+ " possible test cases\n");

		if (excludesDoc != null) {
			System.out.println("THE FOLLOWING EXCLUSIONS WERE ENFORCED:");
			NodeList allTypes = XPathAPI.selectNodeList(excludesDoc,
					"/descendant::hy:type");
			for (int i = 0; i < allTypes.getLength(); i++) {
				Node typeNode = allTypes.item(i);
				String typeName = typeNode.getAttributes().getNamedItem("id")
						.getNodeValue();
				NodeList allExcludesForType = XPathAPI.selectNodeList(typeNode,
						"descendant::hy:exclude");
				for (int j = 0; j < allExcludesForType.getLength(); j++) {
					Node excludeNode = allExcludesForType.item(j);
					String excName = excludeNode.getAttributes().getNamedItem(
							"id").getNodeValue();
					if (excName.equalsIgnoreCase("all")) {
						excName = "!!!!!!!!!!ALL TESTS!!!!!!!!!!";
					}
					System.out.print(typeName + "::" + excName);
					Node reasonNode = XPathAPI.selectSingleNode(excludeNode,
							"descendant::hy:reason");
					if (reasonNode != null) {
						XObject reason = XPathAPI.eval(reasonNode, "string()");
						System.out.print(" (" + reason.str() + ")");
					}
					System.out.print("\n");
				}// end for all excludes
			}// end for all types
			System.out.println("\n");
		}
		super.tearDown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.Test#countTestCases()
	 */
	@Override
    public int countTestCases() {
		return runCases.size();
	}

	private void seekTests(TestSuite suite) {
		Enumeration<?> allTests = suite.tests();
		while (allTests.hasMoreElements()) {
			Test test = (Test) allTests.nextElement();
			seekTests(test);
		}// end while
	}

	private void seekTests(Test test) {
		if (test instanceof TestSuite) {
			seekTests((TestSuite) test);
		} else if (test instanceof TestCase) {
			seekTests((TestCase) test);
		} else if (test instanceof TestDecorator) {
			seekTests(((TestDecorator) test).getTest());
		}
	}

	private void seekTests(TestCase testCase) {
		// Check the testcase against the excludes structure
		if (!isTestCaseExcluded(testCase)) {
			// Add this testcase to the collection of tests to be run.
			runCases.add(testCase);
		}
	}

	private boolean isTestCaseExcluded(TestCase testCase) {
		this.originalTestCaseCount++;
		boolean result = false;
		String tcType = testCase.getClass().getName();
		String tcName = testCase.getName();

		if (excludesDoc != null) {
			try {
				// Find *the first* element for the type. If there are
				// more than one "type" elements for a given type then the
				// first one found is what counts.
				NodeList typeNodes = XPathAPI.selectNodeList(excludesDoc,
						"/descendant::hy:type[@id=\"" + tcType + "\"]");
				if (typeNodes.getLength() > 0) {
					Node typeNode = typeNodes.item(0);
					// Look for a blanket exclusion of this test case type
					NodeList excludes = XPathAPI.selectNodeList(typeNode,
							"descendant::hy:exclude[@id=\"all\"]");
					if (excludes.getLength() != 0) {
						result = true;
					} else {
						// Look for explicit exclusion
						excludes = XPathAPI.selectNodeList(typeNode,
								"descendant::hy:exclude[@id=\"" + tcName
										+ "\"]");
						if (excludes.getLength() != 0) {
							result = true;
						}
					}// end else
				}// end if named type located
			} catch (TransformerException e) {
				// Carry on.
				result = false;
			}
		}
		return result;
	}

	/**
	 */
	private boolean initExcludes() {
		boolean result = true;

		// Look for location of excludes list and (optionally) a schema
		// location override.
		readProperties();
		if (excludesListURI == null) {
			System.out.println("No excludes list specified.");
			result = false;
		}

		if (result) {
			try {
				System.setProperty(DOCUMENT_BUILDER_FACTORY_PROP,
						DOCUMENT_BUILDER_FACTORY_VAL);
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				factory.setNamespaceAware(true);
				factory.setValidating(true);
				factory.setAttribute(JAXP_SCHEMA_LANGUAGE_ATTR_NAME,
						JAXP_SCHEMA_LANGUAGE_ATTR_VAL);
				if (excludesSchemaURI != null) {
					factory.setAttribute(JAXP_SCHEMA_SOURCE_ATTR_NAME,
							excludesSchemaURI);
				}

				DocumentBuilder builder = factory.newDocumentBuilder();
				DocValidator handler = new DocValidator();
				builder.setErrorHandler(handler);
				excludesDoc = builder.parse(excludesListURI);
				if (handler.inError == true) {
					this.docIsValid = false;
					System.out.println("Excludes list document "
							+ excludesListURI + " is invalid :"
							+ handler.inError + " "
							+ handler.spException.getMessage());
					excludesDoc = null;
					result = false;
				} else {
					this.docIsValid = true;
				}
			} catch (FileNotFoundException e) {
				System.out.println("File not found : " + e.getMessage());
				result = false;
			} catch (ParserConfigurationException e) {
				System.out.println("Serious error with XML parser : "
						+ e.getMessage());
				result = false;
			} catch (SAXException e) {
				System.out.println("Serious error occurred parsing document : "
						+ e.getMessage());
				result = false;
			} catch (IOException e) {
				System.out.println("IO error occurred : " + e.getMessage());
				result = false;
			}
		}
		return result;
	}

	/**
	 * 
	 */
	private void readProperties() {
		excludesListURI = System.getProperty(EXCLUDES_FILE_URI_PROP);
		excludesSchemaURI = System.getProperty(EXCLUDES_SCHEMA_URI_PROP);
	}

	/**
	 */
	class DocValidator extends DefaultHandler {
		private boolean inError = false;

		private SAXParseException spException = null;

		@Override
        public void error(SAXParseException exception) throws SAXException {
			inError = true;
			spException = exception;
		}

		@Override
        public void fatalError(SAXParseException exception) throws SAXException {
			inError = true;
			spException = exception;
		}
	}
}
