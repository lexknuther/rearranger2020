/*
 * Copyright (c) 2003, 2010, Dave Kriewall
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.wrq.rearranger.settings.attributeGroups;

import com.wrq.rearranger.entry.MethodEntry;
import com.wrq.rearranger.entry.RangeEntry;
import com.wrq.rearranger.ruleinstance.IRuleInstance;
import com.wrq.rearranger.ruleinstance.InterfaceRuleInstance;
import com.wrq.rearranger.settings.CommentFillString;
import com.wrq.rearranger.settings.CommentRule;
import com.wrq.rearranger.settings.RearrangerSettingsImplementation;
import com.wrq.rearranger.settings.atomicAttributes.NameAttribute;
import com.wrq.rearranger.util.Constraints;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jdom.Element;

/**
 * Rule and attributes to support grouping of methods that implement interface methods.
 */
public class InterfaceAttributes implements AttributeGroup, IRestrictMethodExtraction, IPrioritizableRule {

// ------------------------------ FIELDS ------------------------------

	public static final int METHOD_ORDER_ALPHABETICAL = 0;

	public static final int METHOD_ORDER_INTERFACE_ORDER = 1;

	public static final int METHOD_ORDER_ENCOUNTERED = 2;

	private NameAttribute nameAttr;

	private boolean alphabetizeInterfaces;

	private CommentRule precedingComment;

	private CommentRule trailingComment;

	private boolean noExtractedMethods;

	private int priority;

	private int methodOrder;

	private CommentFillString commentFillString;

// -------------------------- STATIC METHODS --------------------------

	public static /*InterfaceAttributes*/AttributeGroup readExternal(final Element item) {
		InterfaceAttributes result = new InterfaceAttributes();

		result.loadAttributes(item);
		return result;
	}

	public void loadAttributes(final Element item) {
		nameAttr = NameAttribute.readExternal(item);
		alphabetizeInterfaces = RearrangerSettingsImplementation.getBooleanAttribute(item, "alphabetizeInterfaces");
		noExtractedMethods = RearrangerSettingsImplementation.getBooleanAttribute(item, "noExtractedMethods");
		methodOrder = RearrangerSettingsImplementation.getIntAttribute(item, "methodOrder", METHOD_ORDER_ENCOUNTERED);
		Element prc = item.getChild("PrecedingComment");
		if (prc != null) {
			prc = prc.getChild("Comment");
		}
		precedingComment = CommentRule.readExternal(prc);
		Element trc = item.getChild("TrailingComment");
		if (trc != null) {
			trc = trc.getChild("Comment");
		}
		trailingComment = CommentRule.readExternal(trc);
		commentFillString = CommentFillString.readExternal(item);
		priority = RearrangerSettingsImplementation.getIntAttribute(item, "priority", 1);
	}

// --------------------------- CONSTRUCTORS ---------------------------

	public InterfaceAttributes() {
		nameAttr = new NameAttribute();
		noExtractedMethods = false;
		alphabetizeInterfaces = false;
		methodOrder = METHOD_ORDER_ENCOUNTERED;
		precedingComment = new CommentRule();
		trailingComment = new CommentRule();
		commentFillString = new CommentFillString();
		priority = 1;
	}

// --------------------- GETTER / SETTER METHODS ---------------------

	public CommentFillString getCommentFillString() {
		return commentFillString;
	}

	public void setCommentFillString(CommentFillString value) {
		commentFillString = value;
	}

	private JPanel getCommentPanel() {
		JPanel commentPanel = new JPanel(new GridBagLayout());
		Constraints constraints = new Constraints(GridBagConstraints.NORTHWEST);
		Border etchedBorder = BorderFactory.createEtchedBorder();
		TitledBorder titledBorder = BorderFactory.createTitledBorder(etchedBorder, "Comments");

		commentPanel.setBorder(titledBorder);
		constraints.fill = GridBagConstraints.BOTH;

		JLabel legendLabel = new JLabel("Comment Substitutions: use %IF% for interface name");
		JLabel precedingCommentLabel = new JLabel("Preceding Comment:");
		JLabel trailingCommentLabel = new JLabel("Trailing Comment:");
		JPanel precedingCommentPanel = getCommentAreaPanel(getPrecedingComment());
		JPanel trailingCommentPanel = getCommentAreaPanel(getTrailingComment());
		JPanel legendPanel = new JPanel(new GridBagLayout());
		Border border = BorderFactory.createEtchedBorder();

		legendPanel.setBorder(border);
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		legendPanel.add(legendLabel, constraints.lastCol());
		constraints = new Constraints(GridBagConstraints.NORTHWEST);
		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = new Insets(5, 5, 0, 5);
		commentPanel.add(legendPanel, constraints.weightedLastCol());
		constraints.newRow();
		commentPanel.add(precedingCommentLabel, constraints.weightedLastCol());
		constraints.weightedNewRow(0.50d);
		commentPanel.add(precedingCommentPanel, constraints.weightedLastCol());
		constraints.newRow();
		commentPanel.add(trailingCommentLabel, constraints.weightedLastCol());
		constraints.weightedNewRow(0.50d);
		constraints.insets = new Insets(5, 5, 5, 5);
		commentPanel.add(trailingCommentPanel, constraints.weightedLastCol());
		constraints.lastRow();
		constraints.insets = new Insets(5, 5, 0, 5);
		commentPanel.add(commentFillString.getCommentFillStringPanel(), constraints.weightedLastCol());
		return commentPanel;
	}

	private static JPanel getCommentAreaPanel(CommentRule comment) {
		JPanel result = new JPanel(new GridBagLayout());
		Constraints constraints = new Constraints(GridBagConstraints.NORTHWEST);

		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightedLastRow();

		JTextArea commentArea = new JTextArea(4, 40);

		commentArea.setText(comment.getCommentText());

		JScrollPane scrollPane = new JScrollPane(commentArea);

		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		result.add(scrollPane, constraints.weightedLastCol());
		commentArea.getDocument().addDocumentListener(
				new DocumentListener() {

					@Override
					public void changedUpdate(final DocumentEvent event) {
						comment.setCommentText(commentArea.getText());
					}

					@Override
					public void insertUpdate(final DocumentEvent event) {
						comment.setCommentText(commentArea.getText());
					}

					@Override
					public void removeUpdate(final DocumentEvent event) {
						comment.setCommentText(commentArea.getText());
					}

				}
		);
		return result;
	}

	public int getMethodOrder() {
		return methodOrder;
	}

	public void setMethodOrder(int methodOrder) {
		this.methodOrder = methodOrder;
	}

	public NameAttribute getNameAttr() {
		return nameAttr;
	}

	public void setNameAttr(NameAttribute value) {
		nameAttr = value;
	}

	public CommentRule getPrecedingComment() {
		return precedingComment;
	}

	public void setPrecedingComment(CommentRule precedingComment) {
		this.precedingComment = precedingComment;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public void setPriority(int priority) {
		this.priority = priority;
	}

	public CommentRule getTrailingComment() {
		return trailingComment;
	}

	public void setTrailingComment(CommentRule trailingComment) {
		this.trailingComment = trailingComment;
	}

	public boolean isAlphabetizeInterfaces() {
		return alphabetizeInterfaces;
	}

	public void setAlphabetizeInterfaces(boolean alphabetizeInterfaces) {
		this.alphabetizeInterfaces = alphabetizeInterfaces;
	}

	@Override
	public boolean isNoExtractedMethods() {
		return noExtractedMethods;
	}

	@Override
	public void setNoExtractedMethods(boolean noExtractedMethods) {
		this.noExtractedMethods = noExtractedMethods;
	}

// ------------------------ CANONICAL METHODS ------------------------

	public boolean equals(Object value) {
		InterfaceAttributes other;

		return value instanceof InterfaceAttributes &&
				nameAttr.equals((other = (InterfaceAttributes) value).getNameAttr()) &&
				alphabetizeInterfaces == other.alphabetizeInterfaces &&
				methodOrder == other.methodOrder &&
				noExtractedMethods == other.noExtractedMethods &&
				precedingComment.equals(other.precedingComment) &&
				trailingComment.equals(other.trailingComment) &&
				commentFillString.equals(other.commentFillString);
	}

	public final String toString() {
		// convert settings to readable English description of the method.
		//
		final StringBuffer sb = new StringBuffer(80);

		sb.append("Methods implementing interfaces");

		if (nameAttr.isMatch()) {
			sb.append(' ');
			sb.append(nameAttr.getDescriptiveString());
		}

		if (alphabetizeInterfaces) {
			sb.append(" (interfaces alphabetized)");
		}
		if (noExtractedMethods) {
			sb.append(" (no extracted methods)");
		}
		if (precedingComment.getCommentText().length() != 0) {
			sb.append(" Preceding ");
			sb.append(precedingComment.getDescriptiveString());
		}
		if (trailingComment.getCommentText().length() != 0) {
			sb.append(" Trailing ");
			sb.append(trailingComment.getDescriptiveString());
		}
		return sb.toString();
	}

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface AttributeGroup ---------------------

	@Override
	public final /*InterfaceAttributes*/AttributeGroup deepCopy() {
		final InterfaceAttributes result = new InterfaceAttributes();
		result.nameAttr = (NameAttribute) nameAttr.deepCopy();
		result.alphabetizeInterfaces = alphabetizeInterfaces;
		result.noExtractedMethods = noExtractedMethods;
		result.methodOrder = methodOrder;
		result.precedingComment = (CommentRule) precedingComment.deepCopy();
		result.trailingComment = (CommentRule) trailingComment.deepCopy();
		result.commentFillString = commentFillString.deepCopy();
		result.priority = priority;
		return result;
	}

	@Override
	public final void writeExternal(final Element parent) {
		final Element me = new Element("Interface");
		parent.getChildren().add(me);
		nameAttr.appendAttributes(me);
		me.setAttribute("alphabetizeInterfaces", Boolean.valueOf(alphabetizeInterfaces).toString());
		me.setAttribute("noExtractedMethods", Boolean.valueOf(noExtractedMethods).toString());
		me.setAttribute("methodOrder", "" + methodOrder);
		me.setAttribute("priority", "" + priority);
		Element prc = new Element("PrecedingComment");
		Element trc = new Element("TrailingComment");
		me.getChildren().add(prc);
		me.getChildren().add(trc);
		precedingComment.writeExternal(prc);
		trailingComment.writeExternal(trc);
		commentFillString.writeExternal(me);
	}

// --------------------- Interface IRule ---------------------

	@Override
	public void addCommentPatternsToList(List<String> list) {
		if (precedingComment != null) {
			addInterfaceCommentPatternToList(list, precedingComment.getCommentText());
		}
		if (trailingComment != null) {
			addInterfaceCommentPatternToList(list, trailingComment.getCommentText());
		}
	}

	@Override
	public boolean commentsMatchGlobalPattern(String pattern) {
		return getOffendingPatterns(pattern).size() == 0;
	}

	@Override
	public IRuleInstance createRuleInstance() {
		return new InterfaceRuleInstance(this);
	}

	@Override
	public int getCommentCount() {
		int count = 0;
		if (precedingComment != null &&
				precedingComment.getCommentText() != null &&
				precedingComment.getCommentText().length() > 0) {
			count++;
		}
		if (trailingComment != null &&
				trailingComment.getCommentText() != null &&
				trailingComment.getCommentText().length() > 0) {
			count++;
		}
		return count;
	}

	@Override
	public List<String> getOffendingPatterns(String pattern) {
		List<String> result = new ArrayList<String>(2);
		if (precedingComment != null &&
				precedingComment.getCommentText() != null &&
				precedingComment.getCommentText().length() > 0) {
			final String expandedComment = precedingComment.getCommentText().replaceAll(
					"%IF%", "Interface_ExampleName0"
			);
			if (!expandedComment.matches(pattern)) {
				result.add(precedingComment.getCommentText());
			}
		}
		if (trailingComment != null &&
				trailingComment.getCommentText() != null &&
				trailingComment.getCommentText().length() > 0) {
			final String expandedComment = trailingComment.getCommentText().replaceAll(
					"%IF%", "Interface_ExampleName0"
			);
			if (!expandedComment.matches(pattern)) {
				result.add(trailingComment.getCommentText());
			}
		}
		return result;
	}

	@Override
	public final boolean isMatch(RangeEntry rangeEntry) {
		if (rangeEntry instanceof MethodEntry) {
			MethodEntry me = (MethodEntry) rangeEntry;
			if (me.getInterfaceName() != null) {
				return nameAttr.isMatch(me.getInterfaceName());
			}
		}
		return false;
	}

// -------------------------- OTHER METHODS --------------------------

	private void addInterfaceCommentPatternToList(List<String> list, String commentText) {
		if (commentText != null && commentText.length() > 0) {
			String esc = RegexUtil.escape(commentText);
			String fsp = commentFillString.getFillStringPattern();
			esc = esc.replaceAll("%IF%", "[A-Za-z0-9_]+");
			esc = esc.replaceAll("%FS%", fsp);
			list.add(esc);
		}
	}

	public final String getExpandedPrecedingCommentText() {
		return commentFillString.getExpandedCommentText(precedingComment.getCommentText());
	}

	public final String getExpandedTrailingCommentText() {
		return commentFillString.getExpandedCommentText(trailingComment.getCommentText());
	}

	public JPanel getInterfaceAttributes() {
		final JPanel interfacePanel = new JPanel(new GridBagLayout());
		final Border border = BorderFactory.createEtchedBorder();
		Border titleBorder = BorderFactory.createTitledBorder(border, "Match methods implementing interfaces");
		interfacePanel.setBorder(titleBorder);
		final Constraints constraints = new Constraints(GridBagConstraints.NORTHWEST);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(5, 0, 0, 0);
		interfacePanel.add(getNameAttr().getStringPanel(), constraints.weightedLastCol());
		constraints.newRow();
		interfacePanel.add(getAlphabetizePanel(), constraints.weightedLastCol());
		constraints.weightedNewRow();
		interfacePanel.add(getCommentPanel(), constraints.weightedLastCol());
		constraints.newRow();
		interfacePanel.add(getExcludePanel(), constraints.weightedLastCol());
		constraints.lastRow();
		interfacePanel.add(getMethodOrderPanel(), constraints.weightedLastCol());
		return interfacePanel;
	}

	private final JPanel getAlphabetizePanel() {
		final JPanel abPanel = new JPanel(new GridBagLayout());
		final Constraints constraints = new Constraints();
		constraints.weightedLastRow();
		final JCheckBox alphabetizeBox = new JCheckBox("Alphabetize Interfaces");
		abPanel.add(alphabetizeBox, constraints.weightedLastCol());
		alphabetizeBox.setSelected(isAlphabetize());
		alphabetizeBox.addActionListener(
				new ActionListener() {

					@Override
					public void actionPerformed(final ActionEvent e) {
						alphabetizeInterfaces = alphabetizeBox.isSelected();
					}

				}
		);
		return abPanel;
	}

	public boolean isAlphabetize() {
		return alphabetizeInterfaces;
	}

	private JPanel getExcludePanel() {
		JPanel result = new JPanel(new GridBagLayout());
		Constraints constraints = new Constraints(GridBagConstraints.NORTHWEST);

		constraints.lastRow();
		JCheckBox excludeBox = new JCheckBox("Exclude from extracted method processing");

		excludeBox.setSelected(noExtractedMethods);
		excludeBox.addActionListener(event -> noExtractedMethods = excludeBox.isSelected());
		result.add(excludeBox, constraints.weightedLastCol());
		return result;
	}

	private JPanel getMethodOrderPanel() {
		JPanel orderPanel = new JPanel(new GridBagLayout());
		Constraints constraints = new Constraints();
		constraints.weightedLastRow();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = new Insets(0, 5, 0, 0);
		JLabel orderLabel = new JLabel("Order methods");
		orderPanel.add(orderLabel, constraints.firstCol());
		final JComboBox orderBox = new JComboBox(
				new String[]{
						"Alphabetically",
						"In order declared in interface",
						"In order encountered"
				}
		);
		orderBox.setSelectedIndex(methodOrder);
		orderPanel.add(orderBox, constraints.weightedLastCol());
		orderBox.addActionListener(
				new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						methodOrder = orderBox.getSelectedIndex();
					}

				}
		);
		return orderPanel;
	}

}
