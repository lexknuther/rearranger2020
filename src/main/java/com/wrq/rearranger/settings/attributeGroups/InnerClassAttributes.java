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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaToken;
import com.wrq.rearranger.ModifierConstants;
import com.wrq.rearranger.entry.RangeEntry;
import com.wrq.rearranger.settings.atomicAttributes.AbstractAttribute;
import com.wrq.rearranger.settings.atomicAttributes.EnumAttribute;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import org.jdom.Element;

/**
 * Routines to handle inner class modifiers not already covered by ItemAttributes.
 */
public final class InnerClassAttributes
		extends ItemAttributes {

// ------------------------------ FIELDS ------------------------------

	private AbstractAttribute abAttr;

	private EnumAttribute enumAttr;

// -------------------------- STATIC METHODS --------------------------

	public static /*InnerClassAttributes*/AttributeGroup readExternal(final Element item) {
		final InnerClassAttributes result = new InnerClassAttributes();
		CommonAttributes.readExternal(result, item);
		result.abAttr = AbstractAttribute.readExternal(item);
		result.enumAttr = EnumAttribute.readExternal(item);
		return result;
	}

// --------------------------- CONSTRUCTORS ---------------------------

	public InnerClassAttributes() {
		abAttr = new AbstractAttribute();
		enumAttr = new EnumAttribute();
	}

// --------------------- GETTER / SETTER METHODS ---------------------

	private AbstractAttribute getAbAttr() {
		return abAttr;
	}

	public void setAbAttr(AbstractAttribute value) {
		abAttr = value;
	}

	public EnumAttribute getEnumAttr() {
		return enumAttr;
	}

	public void setEnumAttr(EnumAttribute value) {
		enumAttr = value;
	}

// ------------------------ CANONICAL METHODS ------------------------

	public boolean equals(Object value) {
		InnerClassAttributes other;

		return value instanceof InnerClassAttributes &&
				super.equals(other = (InnerClassAttributes) value) &&
				abAttr.equals(other.abAttr) &&
				enumAttr.equals(other.enumAttr);
	}

	public String toString() {
		StringBuffer stringBuffer = new StringBuffer(70);

		stringBuffer.append(abAttr.getDescriptiveString());
		stringBuffer.append(getPlAttr().getProtectionLevelString());
		stringBuffer.append(getStAttr().getDescriptiveString());
		stringBuffer.append(getfAttr().getDescriptiveString());
		stringBuffer.append(enumAttr.getDescriptiveString());
		if (stringBuffer.length() == 0) {
			stringBuffer.append("all inner classes");
		} else {
			stringBuffer.append("inner classes");
		}
		if (getNameAttr().isMatch()) {
			stringBuffer.append(' ');
			stringBuffer.append(getNameAttr().getDescriptiveString());
		}
		stringBuffer.append(getSortAttr().getDescriptiveString());
		return stringBuffer.toString();
	}

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface AttributeGroup ---------------------

	@Override
	public /*InnerClassAttributes*/AttributeGroup deepCopy() {
		InnerClassAttributes result = new InnerClassAttributes();

		deepCopyCommonItems(result);
		result.abAttr = (AbstractAttribute) abAttr.deepCopy();
		result.enumAttr = (EnumAttribute) enumAttr.deepCopy();
		return result;
	}

	@Override
	public void writeExternal(final Element parent) {
		Element me = new Element("InnerClass");

		writeExternalCommonAttributes(me);
		abAttr.appendAttributes(me);
		enumAttr.appendAttributes(me);
		parent.getChildren().add(me);
	}

// --------------------- Interface IRule ---------------------

	@Override
	public boolean isMatch(RangeEntry entry) {
//        return entry.getEnd() instanceof PsiClass   &&
		// entry.getEnd() should be the LBrace of a class.
		return entry.getEnd().getParent() instanceof PsiClass &&
				(entry.getModifiers() & ModifierConstants.ENUM) != 0 ||
				entry.getEnd() instanceof PsiJavaToken &&
						//               ((PsiJavaToken)entry.getEnd()).getTokenType() == PsiJavaToken.LBRACE &&
						entry.getEnd().getText().equals("{") &&
						super.isMatch(entry) &&
						abAttr.isMatch(entry.getModifiers()) &&
						enumAttr.isMatch(entry.getModifiers());
	}

// -------------------------- OTHER METHODS --------------------------

	public JPanel getInnerClassAttributes() {
		JPanel result = new JPanel(new GridBagLayout());
		Border border = BorderFactory.createEtchedBorder();

		result.setBorder(border);

		GridBagConstraints constraints = new GridBagConstraints();

		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.gridheight = 1;
		constraints.weightx = 1.0d;
		constraints.weighty = 0.0d;
		constraints.gridx = constraints.gridy = 0;
		constraints.insets = new Insets(0, 0, 5, 0);
		result.add(getPlAttr().getProtectionLevelPanel(), constraints);
		constraints.gridy = 1;
		constraints.gridheight = 1;
		result.add(getStAttr().getAndNotPanel(), constraints);
		constraints.gridy++;
		result.add(getAbAttr().getAndNotPanel(), constraints);
		constraints.gridy++;
		result.add(getfAttr().getAndNotPanel(), constraints);
		constraints.gridy++;
		result.add(getEnumAttr().getAndNotPanel(), constraints);
		constraints.gridy++;
		result.add(getNameAttr().getStringPanel(), constraints);
		constraints.gridy++;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		constraints.weighty = 1.0d;
		constraints.insets = new Insets(0, 0, 0, 0);
		result.add(getSortAttr().getSortOptionsPanel(), constraints);
		return result;
	}

}

