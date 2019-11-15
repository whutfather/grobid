package org.grobid.trainer.sax;

import org.grobid.core.utilities.TextUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * SAX parser for the TEI format for monograph data. Normally all training data should be in this unique format.
 * The segmentation of tokens must be identical as the one from pdf2xml files so that
 * training and online input tokens are aligned.
 *
 * @author Patrice Lopez
 */
public class TEIMonographSaxParser extends DefaultHandler {

    //private Stack<StringBuffer> accumulators = null; // accumulated parsed piece of texts
    private StringBuffer accumulator = null; // current accumulated text

    private String output = null;
    private Stack<String> currentTags = null;

    //private String fileName = null;
    //private String pdfName = null;

    private int totalReferences = 0;

    private ArrayList<String> labeled = null; // store line by line the labeled data

    public TEIMonographSaxParser() {
        labeled = new ArrayList<String>();
        currentTags = new Stack<String>();
        //accumulators = new Stack<StringBuffer>();
        accumulator = new StringBuffer();
    }

    public void characters(char[] buffer, int start, int length) {
        //if (accumulator != null)
        accumulator.append(buffer, start, length);
        System.out.println(accumulator.toString());
    }

    public String getText() {
        if (accumulator != null) {
            return accumulator.toString().trim();
        } else {
            return null;
        }
    }

    public ArrayList<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(java.lang.String uri,
                           java.lang.String localName,
                           java.lang.String qName) throws SAXException {
        if ((!qName.equals("lb")) & (!qName.equals("pb"))) {
            writeData(qName, true);
        }
    }

    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts)
            throws SAXException {
        if (qName.equals("lb")) {
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } else if (qName.equals("div")) { // if the informations are attached as attributes of div parts
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("preface")) {
                            currentTags.push("<preface>");
                        } else if (value.equals("bibliogr")){
                            currentTags.push("<reference>");
                        } else if (value.equals("ack")){
                            currentTags.push("<dedication>");
                        } else if (value.equals("contents")){
                            currentTags.push("<toc>");
                        } else if (value.equals("appendix")){
                            currentTags.push("<annex>");
                        } else if (value.equals("index")){
                            currentTags.push("<index>");
                        } else if (value.equals("glossary")){
                            currentTags.push("<glossary>");
                        } else if (value.equals("part")){
                            currentTags.push("<unit>");
                        } else if (value.equals("chapter")){
                            currentTags.push("<unit>");
                        }
                    }
                }
            }
        } else {
            // we have to write first what has been accumulated yet with the upper-level tag
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    writeData(qName, false);
                }
            }
            // the tags based on the tags defined in the TaggingLabels class for monograph model (17 labels)
            accumulator.setLength(0);
            totalReferences++;
            if (qName.equals("cover")) {
                currentTags.push("<cover>");
            } else if (qName.equals("title")) {
                currentTags.push("<title>");
            } else if (qName.equals("publisher")) {
                currentTags.push("<publisher>");
            } else if (qName.equals("summary")) {
                currentTags.push("<summary>");
            } else if (qName.equals("biography")) {
                currentTags.push("<biography>");
            } else if (qName.equals("advertisement")) {
                currentTags.push("<advertisement>");
            } else if (qName.equals("toc")) {
                currentTags.push("<toc>");
            } else if (qName.equals("tof")) {
                currentTags.push("<tof>");
            } else if (qName.equals("preface")) {
                currentTags.push("<preface>");
            } else if (qName.equals("dedication")) {
                currentTags.push("<dedication>");
            }else if (qName.equals("unit")) {
                currentTags.push("<unit>");
            }else if (qName.equals("reference")) {
                currentTags.push("<reference>");
            }else if (qName.equals("annex")) {
                currentTags.push("<annex>");
            }else if (qName.equals("index")) {
                currentTags.push("<index>");
            }else if (qName.equals("glossary")) {
                currentTags.push("<glossary>");
            }else if (qName.equals("back")) {
                currentTags.push("<back>");
            }else if (qName.equals("other")) {
                currentTags.push("<other>");
            }
        }
    }

    private void writeData(String qName, boolean pop) {
        if (qName.equals("div") || (qName.equals("cover")) || (qName.equals("title"))
            || (qName.equals("publisher")) || (qName.equals("summary")) || (qName.equals("biography"))
            || (qName.equals("advertisement")) || (qName.equals("toc")) || (qName.equals("tof"))
            || (qName.equals("preface")) || (qName.equals("dedication")) || (qName.equals("unit"))
            || (qName.equals("reference")) || (qName.equals("annex")) || (qName.equals("index"))
            || (qName.equals("glossary")) || (qName.equals("back")) || (qName.equals("other"))) {
            String currentTag = null;
            if (pop) {
                currentTag = currentTags.pop();
            } else {
                currentTag = currentTags.peek();
            }
            String text = getText();
            // we segment the text
            StringTokenizer st = new StringTokenizer(text, " \n\t" + TextUtilities.fullPunctuations, true);
            boolean begin = true;
            while (st.hasMoreTokens()) {
                String tok = st.nextToken().trim();
                if (tok.length() == 0) continue;

                if (tok.equals("+L+")) {
                    labeled.add("@newline\n");
                } else if (tok.equals("+PAGE+")) {
                    // page break should be a distinct feature
                    labeled.add("@newpage\n");
                } else {
                    String content = tok;
                    if (content.length() > 0) {
                        if (begin) {
                            labeled.add(content + " I-" + currentTag + "\n");
                            begin = false;
                        } else {
                            labeled.add(content + " " + currentTag + "\n");
                        }
                    }
                }
                begin = false;
            }
            accumulator.setLength(0);
        }
    }
    public int getTotalReferences() {
        return totalReferences;
    }
}
