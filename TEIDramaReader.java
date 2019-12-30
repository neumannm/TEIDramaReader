/*******************************************************************
 * Copyright (C) 2006, 2007 
 * Linguistic Data Processing, University of Cologne
 * http://www.spinfo.uni-koeln.de
 * 
 * All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, 
 * MA  02111-1307, USA. 

 ********************************************************************/
package de.uni_koeln.spinfo.tesla.component.reader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.tika.io.IOUtils;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.uni_koeln.spinfo.tesla.annotation.adapter.IAnnotationFactory;
import de.uni_koeln.spinfo.tesla.annotation.adapter.IOutputAdapter;
import de.uni_koeln.spinfo.tesla.annotation.adapter.TypeMapping;
import de.uni_koeln.spinfo.tesla.annotation.adapter.hibernate.DefaultHibernateOutputAdapter;
import de.uni_koeln.spinfo.tesla.annotation.adapter.tunguska.DefaultTunguskaOutputAdapter;
import de.uni_koeln.spinfo.tesla.component.reader.drama.DramaAccessAdapter;
import de.uni_koeln.spinfo.tesla.component.reader.drama.TEIDramaAct;
import de.uni_koeln.spinfo.tesla.component.reader.drama.TEIDramaScene;
import de.uni_koeln.spinfo.tesla.component.reader.drama.TEIDramaSpeaker;
import de.uni_koeln.spinfo.tesla.component.reader.drama.TEIDramaSpeechAct;
import de.uni_koeln.spinfo.tesla.component.reader.drama.TEIDramaStage;
import de.uni_koeln.spinfo.tesla.roles.dc.data.IDublinCoreMetadata;
import de.uni_koeln.spinfo.tesla.roles.dc.impl.hibernate.DublinCoreMetaDataAccessAdapterImpl;
import de.uni_koeln.spinfo.tesla.roles.dc.impl.hibernate.DublinCoreMetaDataImpl;
import de.uni_koeln.spinfo.tesla.runtime.component.annotations.Author;
import de.uni_koeln.spinfo.tesla.runtime.component.annotations.Description;
import de.uni_koeln.spinfo.tesla.runtime.component.annotations.Licence;
import de.uni_koeln.spinfo.tesla.runtime.component.annotations.OutputAdapter;
import de.uni_koeln.spinfo.tesla.runtime.component.annotations.Reader;
import de.uni_koeln.spinfo.tesla.runtime.component.annotations.RoleDescription;
import de.uni_koeln.spinfo.tesla.runtime.persistence.Annotation;

/**
 * Reader-Komponente, die XML-Dokumente akzeptiert, welche nach TEI
 * ausgezeichnet sind. Vorgesehen ist die Komponente für das Lesen von
 * Dramentexten.
 * 
 * Folgende Annotationen werden produziert: Akte (<div type="act">), Szenen
 * (<div type="scene">), Sprechakte (<sp>), Sprecher (<speaker>) und
 * Bühnenanweisungen (<stage>). Zusätzlich werden Metadaten im Dublin Core
 * Format geliefert.
 * 
 * 
 * @author neumannm
 * 
 */
@Reader(author = { @Author(author = "Mandy Neumann", email = "mandy.neumann@uni-koeln.de", organization = "Sprachliche Informationsverarbeitung") }, description = @Description(name = "TEI Drama Reader", licence = Licence.GPL_3, summary = "Reads Files in TEI Format Encoding Performance Texts", bigO = "linear (number of annotations)", version = "1.0", reusableResults = true), level = 80)
public class TEIDramaReader extends TextReader {

	@OutputAdapter(dataObject = DublinCoreMetaDataImpl.class, type = DefaultHibernateOutputAdapter.class, name = "DCMetadata", accessAdapterImpl = DublinCoreMetaDataAccessAdapterImpl.class)
	@RoleDescription(value = "de.uni_koeln.spinfo.tesla.rolesystem.presets.roles.DublinCoreMetadataGenerator")
	private IOutputAdapter<DublinCoreMetaDataImpl> metaDataOutputAdapter;

	@OutputAdapter(dataObject = TEIDramaAct.class, type = DefaultTunguskaOutputAdapter.ProtoStuff.class, name = "Acts", accessAdapterImpl = DramaAccessAdapter.class)
	@RoleDescription("de.uni_koeln.spinfo.tesla.roles.teiDramaActDetector")
	private static IOutputAdapter<TEIDramaAct> actWriter;

	@OutputAdapter(dataObject = TEIDramaScene.class, type = DefaultTunguskaOutputAdapter.ProtoStuff.class, name = "Scenes", accessAdapterImpl = DramaAccessAdapter.class)
	@RoleDescription("de.uni_koeln.spinfo.tesla.roles.teiDramaSceneDetector")
	private IOutputAdapter<TEIDramaScene> sceneWriter;

	@OutputAdapter(dataObject = TEIDramaSpeechAct.class, type = DefaultTunguskaOutputAdapter.ProtoStuff.class, name = "SpeechActs", accessAdapterImpl = DramaAccessAdapter.class)
	@RoleDescription("de.uni_koeln.spinfo.tesla.roles.teiDramaSpeechActDetector")
	private IOutputAdapter<TEIDramaSpeechAct> speechActWriter;

	@OutputAdapter(dataObject = TEIDramaSpeaker.class, type = DefaultTunguskaOutputAdapter.ProtoStuff.class, name = "Speakers", accessAdapterImpl = DramaAccessAdapter.class)
	@RoleDescription("de.uni_koeln.spinfo.tesla.roles.teiDramaSpeakerDetector")
	private IOutputAdapter<TEIDramaSpeaker> speakerWriter;

	@OutputAdapter(dataObject = TEIDramaStage.class, type = DefaultTunguskaOutputAdapter.ProtoStuff.class, name = "StageDirections", accessAdapterImpl = DramaAccessAdapter.class)
	@RoleDescription("de.uni_koeln.spinfo.tesla.roles.teiDramaStageDirectionDetector")
	private IOutputAdapter<TEIDramaStage> stageWriter;

	private static final long serialVersionUID = -8987805958223967437L;

	private DublinCoreMetaDataImpl metaData;
	private IAnnotationFactory actAnnotationfactory;
	private IAnnotationFactory sceneAnnotationfactory;
	private IAnnotationFactory speechActAnnotationfactory;
	private IAnnotationFactory speakerAnnotationfactory;
	private IAnnotationFactory stageAnnotationfactory;

	/**
	 * Liefert eine Vorschau des Texts, wie er von diesem Reader ausgegeben
	 * wird, ohne dabei Annotationen zu schreiben.
	 * 
	 */
	public void getPreview(InputStream input, OutputStream output,
			String encoding) throws IOException {
		processText(input, output, false, "", encoding);
	}

	/**
	 * Prozessierende Methode. Verarbeitet den InputStream ereignisbasiert mit
	 * Hilfe von StAX und reagiert dabei auf die vordefinierten Ereignisse.
	 * Metadaten werden separat ausgelesen.
	 */
	protected void processText(InputStream input, OutputStream output,
			boolean writeAnnotations, String docId, String encoding)
			throws IOException {

		OutputStreamWriter osw = null;

		IAnnotationFactory metadataAnnotationFactory = null;

		if (writeAnnotations) {
			metadataAnnotationFactory = metaDataOutputAdapter
					.getAnnotationFactory();
			actAnnotationfactory = actWriter.getAnnotationFactory();
			sceneAnnotationfactory = sceneWriter.getAnnotationFactory();
			speechActAnnotationfactory = speechActWriter.getAnnotationFactory();
			speakerAnnotationfactory = speakerWriter.getAnnotationFactory();
			stageAnnotationfactory = stageWriter.getAnnotationFactory();
		}

		XMLInputFactory factory = XMLInputFactory.newInstance();

		try {
			int offset = 0;
			osw = new OutputStreamWriter(output, "UTF-8");

			byte[] bs = IOUtils.toByteArray(input);

			metaData = queryMetaData(new ByteArrayInputStream(bs));

			XMLEventReader reader = factory
					.createXMLEventReader(new ByteArrayInputStream(bs), encoding);

			while (reader.hasNext()) {
				XMLEvent event = reader.nextEvent();

				if (event.isStartElement()) {
					QName startEventName = event.asStartElement().getName();

					if (TEIDramaElements.TEXT.equals(startEventName)) {
						offset = parseTEIText(reader, docId, osw, offset);
					}
				}

				else if (event.isEndElement()) {
					if (TEIDramaElements.TEI.equals(event.asEndElement()
							.getName())) {
						break;
					}
				}
			}

			if (metadataAnnotationFactory != null) {
				Annotation<DublinCoreMetaDataImpl> anno = metadataAnnotationFactory
						.newAnnotation(0, offset, docId, metaData,
								TypeMapping.NONE);
				metaDataOutputAdapter.store(anno);
			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}

		finally {
			try {
				osw.close();
			} catch (IOException e) {
				throw new IOException(e);
			}

		}
	}

	/**
	 * Liest Metadaten aus und speichert sie in den entsprechenden Feldern des
	 * Dublin Core.
	 */
	@Override
	public IDublinCoreMetadata getMetaData(InputStream in, String encoding) {

		DublinCoreMetaDataImpl meta = new DublinCoreMetaDataImpl();
		try {
			meta = queryMetaData(in);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return meta;
	}

	/*
	 * Diese Methode erledigt das Einlesen der Metadaten aus den entsprechenden
	 * Feldern im TEI-Header mit Hilfe von XPath-Ausdrücken. Dabei gibt es zu
	 * den Feldern type und coverage in TEI kein Äquivalent. Für andere Felder
	 * bestehen mitunter mehrere mögliche Äquivalente in TEI.
	 */
	private DublinCoreMetaDataImpl queryMetaData(InputStream in)
			throws ParserConfigurationException, SAXException, IOException,
			XPathExpressionException {
		DublinCoreMetaDataImpl meta = new DublinCoreMetaDataImpl();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		org.w3c.dom.Document doc = builder.parse(in);

		XPathFactory xFactory = XPathFactory.newInstance();
		XPath xpath = xFactory.newXPath();
		HashMap<String, String> nsMap = new HashMap<String, String>();
		nsMap.put("", "http://www.tei-c.org/ns/1.0");

		xpath.setNamespaceContext(new NamespaceContext() {
			public String getNamespaceURI(String prefix) {
				if (prefix == null)
					throw new NullPointerException("Null prefix");
				else if ("".equals(prefix))
					return "http://www.tei-c.org/ns/1.0";
				return XMLConstants.NULL_NS_URI;
			}

			public String getPrefix(String uri) {
				throw new UnsupportedOperationException();
			}

			public Iterator getPrefixes(String uri) {
				throw new UnsupportedOperationException();
			}
		});

		XPathExpression expr = null;
		String result = null;
		String contributors = new String();
		String sources = new String();
		String relations = new String();

		expr = xpath.compile("//:teiHeader/:fileDesc/:titleStmt/:title");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		meta.setTitle(result);

		expr = xpath.compile("//:teiHeader/:fileDesc/:titleStmt/:author");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		meta.setCreator(result);

		expr = xpath
				.compile("//:teiHeader/:profileDesc/:textClass/:keywords/:list/:item");
		NodeList resultSet = (NodeList) expr.evaluate(doc,
				XPathConstants.NODESET);
		result = new String();
		for (int i = 0; i < resultSet.getLength(); i++) {
			if (!result.isEmpty())
				result += ", ";
			result += resultSet.item(i).getNodeValue();
		}
		meta.setSubject(result);

		expr = xpath.compile("//:teiHeader/:encodingDesc/:projectDesc");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		meta.setDescription(result);

		expr = xpath
				.compile("//:teiHeader/:fileDesc/:publicationStmt/:publisher");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		meta.setPublisher(result);

		expr = xpath.compile("//:teiHeader/:fileDesc/:publicationStmt/:date");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		meta.setDate(result);

		expr = xpath.compile("//:teiHeader/:profileDesc/:langUsage/:language");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		meta.setLanguage(result);

		expr = xpath
				.compile("//:teiHeader/:fileDesc/:publicationStmt/:availability");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		meta.setRights(result);

		expr = xpath.compile("//:teiHeader/:fileDesc/:titleStmt/:editor");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		if (!contributors.isEmpty())
			contributors += ", ";
		contributors += result;

		expr = xpath.compile("//:teiHeader/:fileDesc/:publicationStmt/:idno");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		meta.setIdentifier(result);

		expr = xpath.compile("//:teiHeader/:fileDesc/:seriesStmt/:title");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		if (!relations.isEmpty())
			relations += ", ";
		relations += result;

		expr = xpath
				.compile("//:teiHeader/:fileDesc/:sourceDesc/:recordingStmt/:recording");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		if (!relations.isEmpty())
			relations += ", ";
		relations += result;

		expr = xpath.compile("//:teiHeader/:fileDesc/:sourceDesc/:bibl");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		if (!sources.isEmpty())
			sources += ", ";
		sources += result;

		expr = xpath.compile("//:teiHeader/:fileDesc/:sourceDesc/:biblFull");
		result = normalizeString((String) expr.evaluate(doc,
				XPathConstants.STRING));
		if (!sources.isEmpty())
			sources += ", ";
		sources += result;

		meta.setFormat("text/XML");
		meta.setContributor(contributors);
		meta.setSource(sources);
		meta.setRelation(relations);

		return meta;
	}

	/*
	 * Hilfsmethode zur Normalisierung von Strings, um sie von überflüssigen
	 * Tabs und Zeilenumbrüchen zu bereinigen.
	 */
	private String normalizeString(String string) {
		return string.replaceAll("\\t", "").replaceAll("\\n", " ").trim();
	}

	/**
	 * 
	 * @see de.uni_koeln.spinfo.tesla.component.reader.TextReader#supportsContent(java.io.InputStream)
	 */
	public boolean supportsContent(InputStream input) {
		BufferedReader in = new BufferedReader(new InputStreamReader(input));
		boolean result = testSupport(in);

		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;

	}

	/*
	 * Hilfsmethode zum Testen der Unterstützung. Der Stream wird zeilenweise
	 * gelesen und die ersten Zeilen werden auf das Vorhandensein der XML-PI
	 * sowie des Wurzelelements <TEI> mit entsprechendem TEI-Namespace
	 * überprüft.
	 */
	private boolean testSupport(BufferedReader in) {
		try {
			String s = in.readLine();
			if (s == null) {
				return false;
			}
			while (s.trim().isEmpty()) {
				s = in.readLine();
			}
			if (deleteNullsFromString(s).contains("<?xml version")) {
				s = in.readLine();
				while (s.trim().isEmpty()) {
					s = in.readLine();
				}
			}

			if (deleteNullsFromString(s).contains("<TEI")
					&& deleteNullsFromString(s).contains(
							"xmlns=\"http://www.tei-c.org/ns/1.0\"")) {
				return true;
			}

			return false;
		} catch (IOException e) {
			return false;
		}
	}

	private int parseTEIText(XMLEventReader reader, String docId,
			OutputStreamWriter osw, int offset) throws XMLStreamException,
			IOException {

		while (reader.hasNext()) {

			XMLEvent event = reader.nextEvent();

			if (event.isStartElement()) {
				StartElement startElement = event.asStartElement();
				QName startElementName = startElement.getName();

				if (TEIDramaElements.DIV.equals(startElementName)
						|| TEIDramaElements.DIV1.equals(startElementName)
						|| TEIDramaElements.DIV2.equals(startElementName)) {
					Attribute divType = startElement
							.getAttributeByName(new QName("type"));

					if (divType != null) {
						if (divType.getValue().equals("act")) {
							offset = parseTEIDramaAct(reader, docId, osw,
									offset);
							continue;
						} else {
							reader.nextEvent();
						}
					} else {
						reader.nextEvent();
					}
				}

			} else if (event.isCharacters()) {
				Characters characters = event.asCharacters();
				if (!characters.isWhiteSpace()) {
					String text = normalizeString(characters.getData());
					osw.append(text + "\n");
					offset += text.length() + "\n".length();
				}
			} else if (event.isEndElement()) {
				if (TEIDramaElements.TEXT
						.equals(event.asEndElement().getName())) {
					break;
				}
			}
		}
		return offset;
	}

	/*
	 * Methode zum Parsen eines Aktes. Sie wird aufgerufen, wenn ein <div> mit
	 * dem Wert "act" für das @type-Attribut angetroffen wurde. Ein Akt soll als
	 * Label seinen Namen erhalten. Dieser kann entweder im Element <head>
	 * vermerkt werden, was eine beschreibende Annotation ist, oder in <title>,
	 * was eine tatsächlich im gedruckten Werk vorhandene Überschrift darstellt.
	 * 
	 * Innerhalb des Akts wird auf ein Ereignis gewartet, das das Parsen einer
	 * Szene auslöst, also das Auftreten eines div> mit dem Wert "scene" für das
	 * 
	 * @type-Attribut. Zeichenketten, die außerhalb dieses Elements auftauchen
	 * werden direkt in den OutputStream geschrieben.
	 * 
	 * Die Methode beendet das Parsing und gibt die Kontrolle an die aufrufende
	 * Methode zurück, wenn das <div> für den Akt geschlossen wird. Da innerhalb
	 * dieses <div> beliebig viele weitere <div> auftauchen können, wird über
	 * einen Zähler das korrekte Endelement bestimmt.
	 */
	private int parseTEIDramaAct(XMLEventReader reader, String docId,
			OutputStreamWriter osw, int offset) throws XMLStreamException,
			IOException {

		int counter = 1;
		int right = offset;

		TEIDramaAct act = new TEIDramaAct();

		while (reader.hasNext()) {
			XMLEvent event = reader.nextEvent();

			if (event.isStartElement()) {
				StartElement startElement = event.asStartElement();
				QName startElementName = startElement.getName();

				/*
				 * Der Titel eines Akts enthält oftmals, sofern vorhanden, eine
				 * Ziffer (bspw. "1. Akt" oder "Akt 1").
				 */
				if (TEIDramaElements.TITLE.equals(startElementName)) {
					if (reader.peek().isCharacters()) {
						String title = reader.peek().asCharacters().getData();
						for (int i = 0; i < title.length(); i++) {
							if (Character.isDigit(title.charAt(i))) {
								act.setNumber(Character.digit(title.charAt(i),
										10));
							}
						}
						if (act.getLabel() == null || act.getLabel().isEmpty())
							act.setLabel(title);
					}
				}

				if (TEIDramaElements.HEAD.equals(startElementName)) {
					if (reader.peek().isCharacters()) {
						String header = reader.peek().asCharacters().getData();
						if (act.getLabel() == null || act.getLabel().isEmpty())
							act.setLabel(header);
					}
				}

				if (TEIDramaElements.DIV.equals(startElementName)
						|| TEIDramaElements.DIV1.equals(startElementName)
						|| TEIDramaElements.DIV2.equals(startElementName)) {
					counter++;
					Attribute divType = startElement
							.getAttributeByName(new QName("type"));

					if (divType != null) {
						if (divType.getValue().equals("scene")) {
							right = parseTEIDramaScene(reader, docId, osw,
									right);
						} else {
							reader.nextEvent();
						}
					} else {
						reader.nextEvent();
					}
				}
			}

			else if (event.isEndElement()) {
				QName endElementName = event.asEndElement().getName();

				if (TEIDramaElements.DIV.equals(endElementName)
						|| TEIDramaElements.DIV1.equals(endElementName)
						|| TEIDramaElements.DIV2.equals(endElementName)) {
					counter--;
					if (counter == 0) {
						if (actAnnotationfactory != null) {
							Annotation<TEIDramaAct> anno = actAnnotationfactory
									.newAnnotation(offset, right, docId, act,
											TypeMapping.NONE);
							actWriter.store(anno);
						}
						break;
					}
				}
			}

			else if (event.isCharacters()) {
				Characters characters = event.asCharacters();
				if (!characters.isWhiteSpace()) {
					String text = normalizeString(characters.getData());
					osw.append(text + "\n");
					right += text.length() + "\n".length();
				}
			}
		}
		return right;
	}

	/*
	 * Methode zum Parsen einer Szene. Sie wird aufgerufen, wenn ein <div> mit
	 * dem Wert "scene" für das @type-Attribut angetroffen wurde. Eine Szene
	 * soll als Label seinen Namen erhalten. Dieser kann entweder im Element
	 * <head> vermerkt werden, was eine beschreibende Annotation ist, oder in
	 * <title>, was eine tatsächlich im gedruckten Werk vorhandene Überschrift
	 * darstellt.
	 * 
	 * Innerhalb der Szene wird auf Ereignisse gewartet, das das Parsen eines
	 * Sprechakts auslöst, also das Auftreten eines <sp>-Elements, oder einer
	 * Bühnenanweisung (<stage>). Zeichenketten, die außerhalb dieser Elemente
	 * auftauchen werden direkt in den OutputStream geschrieben.
	 * 
	 * Die Methode beendet das Parsing und gibt die Kontrolle an die aufrufende
	 * Methode zurück, wenn das <div> für die Szene geschlossen wird. Da
	 * innerhalb dieses <div> beliebig viele weitere <div> auftauchen können,
	 * wird über einen Zähler das korrekte Endelement bestimmt.
	 */
	private int parseTEIDramaScene(XMLEventReader reader, String docId,
			OutputStreamWriter osw, int offset) throws XMLStreamException,
			IOException {

		int counter = 1;
		int right = offset;
		int stageLeft = offset;

		TEIDramaScene scene = new TEIDramaScene();
		TEIDramaStage stage = null;

		while (reader.hasNext()) {

			if (reader.peek().isEndElement()) {
				QName endElementName = reader.peek().asEndElement().getName();

				if (TEIDramaElements.DIV.equals(endElementName)
						|| TEIDramaElements.DIV1.equals(endElementName)
						|| TEIDramaElements.DIV2.equals(endElementName)) {
					counter--;
					if (counter == 0) {
						if (sceneAnnotationfactory != null) {
							Annotation<TEIDramaScene> anno = sceneAnnotationfactory
									.newAnnotation(offset, right, docId, scene,
											TypeMapping.NONE);
							sceneWriter.store(anno);
						}
						osw.append("\n");
						right += "\n".length();
						break;
					}
				}
			}

			XMLEvent event = reader.nextEvent();

			if (event.isStartElement()) {
				QName startElementName = event.asStartElement().getName();

				if (TEIDramaElements.TITLE.equals(startElementName)) {
					if (reader.peek().isCharacters()) {
						String header = reader.peek().asCharacters().getData();
						for (int i = 0; i < header.length(); i++) {
							if (Character.isDigit(header.charAt(i))) {
								scene.setNumber(Character.digit(
										header.charAt(i), 10));
								scene.setLabel("Scene " + scene.getNumber());
							}
						}
						if (scene.getLabel() == null
								|| scene.getLabel().isEmpty())
							scene.setLabel(header);
					}
				}

				if (TEIDramaElements.HEAD.equals(startElementName)) {
					if (reader.peek().isCharacters()) {
						String header = reader.peek().asCharacters().getData();
						if (scene.getLabel() == null
								|| scene.getLabel().isEmpty())
							scene.setLabel(header);
					}
				}

				if (TEIDramaElements.DIV.equals(startElementName)
						|| TEIDramaElements.DIV1.equals(startElementName)
						|| TEIDramaElements.DIV2.equals(startElementName)) {
					counter++;
				}

				if (TEIDramaElements.SPEECH.equals(startElementName)) {
					right = parseTEIDramaSpeechAct(reader, docId, osw, right);
				}

				if (TEIDramaElements.STAGE.equals(startElementName)) {
					stageLeft = right;
					stage = new TEIDramaStage();
					String text = normalizeString(readCharacters(reader,
							TEIDramaElements.STAGE));
					stage.setText(text);
					osw.append(text + "\n");
					right += text.length() + "\n".length();
				}
			}

			else if (event.isEndElement()) {
				QName endElementName = event.asEndElement().getName();

				if (TEIDramaElements.STAGE.equals(endElementName)) {
					if (stageAnnotationfactory != null) {
						Annotation<TEIDramaStage> anno = stageAnnotationfactory
								.newAnnotation(stageLeft, right, docId, stage,
										TypeMapping.NONE);
						stageWriter.store(anno);
					}
				}
			}

			else if (event.isCharacters()) {
				Characters characters = event.asCharacters();
				if (!characters.isWhiteSpace()) {
					String text = normalizeString(characters.getData());
					osw.append(text + "\n");
					right += text.length() + "\n".length();
				}
			}
		}
		return right;
	}

	/*
	 * Methode zum Parsen eines Sprechakts. Sie wird aufgerufen, wenn das
	 * Element <sp> in einer Szene angetroffen wurde.
	 * 
	 * Innerhalb des Sprechakts können der Name des Sprechers in <speaker>,
	 * Bühnenanweisungen in <stage> sowie der gesprochene Text in Textelementen
	 * wie <l> oder <p> auftauchen. wird auf Ereignisse gewartet, das das Parsen
	 * eines Sprechakts auslöst, also das Auftreten eines <sp>-Elements, oder
	 * einer Bühnenanweisung (<stage>).
	 * 
	 * Die Methode beendet das Parsing und gibt die Kontrolle an die aufrufende
	 * Methode zurück, wenn das <sp> Element geschlossen wird.
	 */
	private int parseTEIDramaSpeechAct(XMLEventReader reader, String docId,
			OutputStreamWriter osw, int offset) throws XMLStreamException,
			IOException {
		int right = offset;
		int speakerLeft = offset;
		int stageLeft = offset;

		TEIDramaSpeechAct speechAct = new TEIDramaSpeechAct();
		TEIDramaSpeaker speaker = null;
		TEIDramaStage stage = null;

		while (reader.hasNext()) {

			XMLEvent event = reader.nextEvent();

			if (event.isStartElement()) {
				QName subElementName = event.asStartElement().getName();

				if (TEIDramaElements.SPEAKER.equals(subElementName)) {
					speakerLeft = right;
					speaker = new TEIDramaSpeaker();
					String name = readCharacters(reader,
							TEIDramaElements.SPEAKER);
					speaker.setName(name);
					osw.append(name + " ");
					right += name.length() + " ".length();
				}

				if (TEIDramaElements.STAGE.equals(subElementName)) {
					stageLeft = right;
					stage = new TEIDramaStage();
					String text = normalizeString(readCharacters(reader,
							TEIDramaElements.STAGE));
					stage.setText(text);
					osw.append(text + "\n");
					right += text.length() + "\n".length();
				}

				if (TEIDramaElements.PARAGRAPH.equals(subElementName)) {
					String text = normalizeString(readCharacters(reader,
							TEIDramaElements.PARAGRAPH));
					speechAct.addText(text);
					osw.append(text + "\n");
					right += text.length() + "\n".length();
				}

				if (TEIDramaElements.LINE.equals(subElementName)) {
					String text = normalizeString(readCharacters(reader,
							TEIDramaElements.LINE));
					speechAct.addText(text);
					osw.append(text + "\n");
					right += text.length() + "\n".length();
				}
			}

			else if (event.isEndElement()) {
				QName subElementName = event.asEndElement().getName();

				if (TEIDramaElements.SPEAKER.equals(subElementName)) {
					if (speakerAnnotationfactory != null) {
						Annotation<TEIDramaSpeaker> anno = speakerAnnotationfactory
								.newAnnotation(speakerLeft, right, docId,
										speaker, TypeMapping.NONE);
						speakerWriter.store(anno);
					}
				}

				if (TEIDramaElements.STAGE.equals(subElementName)) {
					if (stageAnnotationfactory != null) {
						Annotation<TEIDramaStage> anno = stageAnnotationfactory
								.newAnnotation(stageLeft, right, docId, stage,
										TypeMapping.NONE);
						stageWriter.store(anno);
					}
				}

				if (TEIDramaElements.SPEECH.equals(subElementName)) {
					if (speechActAnnotationfactory != null) {
						Annotation<TEIDramaSpeechAct> anno = speechActAnnotationfactory
								.newAnnotation(offset, right, docId,
										speechAct, TypeMapping.NONE);
						speechActWriter.store(anno);
					}
					break;
				}
			}
		}
		return right;
	}

	/*
	 * Allgemeine Methode zum Einlesen aller Zeichenketten innerhalb eines
	 * bestimmten Elements bis zum Ende dieses Elements. Zeichenketten von
	 * Kindelementen werden dabei ebenfalls eingelesen und an den String
	 * angefügt.
	 */
	private String readCharacters(XMLEventReader reader, QName currentElement)
			throws XMLStreamException {
		String string = new String();

		while (reader.hasNext()) {
			if (reader.peek().isEndElement()
					&& reader.peek().asEndElement().getName()
							.equals(currentElement)) {
				break;
			}

			XMLEvent event = reader.nextEvent();

			if (event.isCharacters()) {
				Characters characters = event.asCharacters();
				if (!string.isEmpty())
					string += " ";
				string += characters.getData().trim();
			}
		}

		return string;
	}

	public static class TEIDramaElements {
		private final static String NAMESPACE = "http://www.tei-c.org/ns/1.0";

		private final static QName TEI = new QName(NAMESPACE, "TEI");

		private final static QName TEXT = new QName(NAMESPACE, "text");
		private final static QName DIV = new QName(NAMESPACE, "div");
		private final static QName DIV1 = new QName(NAMESPACE, "div1");
		private final static QName DIV2 = new QName(NAMESPACE, "div2");
		private final static QName SPEECH = new QName(NAMESPACE, "sp");
		private final static QName SPEAKER = new QName(NAMESPACE, "speaker");
		private final static QName PARAGRAPH = new QName(NAMESPACE, "p");
		private final static QName LINE = new QName(NAMESPACE, "l");
		private final static QName STAGE = new QName(NAMESPACE, "stage");
		private final static QName HEAD = new QName(NAMESPACE, "head");
		private final static QName TITLE = new QName(NAMESPACE, "title");
	}
}