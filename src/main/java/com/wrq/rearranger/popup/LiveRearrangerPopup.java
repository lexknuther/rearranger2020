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
package com.wrq.rearranger.popup;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.wrq.rearranger.LiveRearrangerActionHandler;
import com.wrq.rearranger.entry.RangeEntry;
import com.wrq.rearranger.rearrangement.Emitter;
import com.wrq.rearranger.ruleinstance.IRuleInstance;
import com.wrq.rearranger.settings.RearrangerSettings;
import com.wrq.rearranger.settings.RearrangerSettingsImplementation;
import com.wrq.rearranger.util.IconUtil;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.dnd.DnDConstants;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.lang.reflect.InvocationTargetException;

import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.log4j.BasicConfigurator;

/**
 * Contains logic to display a rearrangement popup, allow user to perform drag&drop rearrangement, and (when a keystroke
 * is seen or a mouse click outside the popup occurs) rearrange the code accordingly.
 */
public class LiveRearrangerPopup
		implements IHasScrollPane, ILiveRearranger {

// ------------------------------ FIELDS ------------------------------

	private Logger logger = Logger.getInstance(getClass());

	final RearrangerSettingsImplementation settings;

	PopupTreeComponent treeComponent;

	List<IRuleInstance> resultRuleInstances;

	final Window outerPanel;

	final Document document;

	final PsiFile psiFile;

	IFilePopupEntry psiFileEntry;

	//    private JWindow popup;
	TreeDragSource tds;

	TreeDropTarget tdt;

	boolean rearrangementOccurred;

	int cursorOffset;

	boolean sawKeyPressed;

	private WindowFocusListener windowFocusListener;

	private WindowAdapter windowAdapter;

	private MouseAdapter mouseAdapter;

	private KeyEventDispatcher keyEventDispatcher;

	private Popup popup;

	private FocusAdapter focusAdapter;

	private JPanel containerPanel;

	private final Project project;

	private Cursor oldCursor;

	private MouseAdapter ma2;

	private Component outerPanelFocusOwner;

// --------------------------- CONSTRUCTORS ---------------------------

	public LiveRearrangerPopup(
			RearrangerSettingsImplementation settings,
			final IFilePopupEntry psiFileEntry,
			Window outerPanel,
			Document document,
			Project project) {
		logger.debug("entered LiveRearrangerPopup constructor");
		this.settings = settings;
		this.outerPanel = outerPanel;
		this.psiFileEntry = psiFileEntry;
		this.document = document;
		psiFile = null;
		this.project = project;
	}

	public LiveRearrangerPopup(
			RearrangerSettingsImplementation settings, PsiFile psiFile, Document document, Project project,
			final Window outerPanel, int cursorOffset) {
		this.settings = settings;
		this.document = document;
		this.psiFile = psiFile;
		createFilePopupEntry(psiFile);
		this.project = project;
		this.outerPanel = outerPanel;
		this.cursorOffset = cursorOffset;
		try {
			SwingUtilities.invokeAndWait(
					new Runnable() {

						@Override
						public void run() {
//                            popup = new JWindow();
							JPanel tempPanel = new JPanel(new GridBagLayout());
							JLabel label = new JLabel("Live Rearranger parsing file...");
							Border b = BorderFactory.createRaisedBevelBorder();
							tempPanel.setBorder(b);
							GridBagConstraints constraints = new GridBagConstraints();
							constraints.insets = new Insets(5, 5, 5, 5);
							tempPanel.add(label, constraints);
							Dimension d = outerPanel.getSize();
							Dimension c = tempPanel.getPreferredSize();
							int x = (d.width - c.width) / 2;
							int y = (d.height - c.height) / 2;
							if (x < 0) {
								x = 0;
							}
							if (y < 0) {
								y = 0;
							}
							popup = PopupFactory.getSharedInstance().getPopup(outerPanel, tempPanel, x, y);
//                            popup.getContentPane().add(tempPanel);
//                            popup.setLocation(x, y);
							logger.debug("initial outerPanel size=" + d + ", tempPanel preferred size=" + c);
							logger.debug("Constructing initial Popup at x,y=" + x + "," + y);
//                            popup.pack();
//                            popup.setVisible(true);
//                            popup.requestFocusInWindow();
							popup.show();
							Cursor cu = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
							oldCursor = outerPanel.getCursor();
							logger.debug("setCursor (WAIT)" + cu + " on " + outerPanel);
							outerPanel.setCursor(cu);
						}

					}
			);
		} catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		} catch (InvocationTargetException ite) {
			throw new RuntimeException(ite);
		}
	}

	private void createFilePopupEntry(final PsiFile psiFile) {
		psiFileEntry = new IFilePopupEntry() {

			@Override
			public String getTypeIconName() {
				return "nodes/ppFile";
			}

			@Override
			public String[] getAdditionalIconNames() {
				return null;
			}

			@Override
			public JLabel getPopupEntryText(RearrangerSettings settings) {
				return new JLabel(psiFile.getName());
			}

		};
	}

// --------------------- GETTER / SETTER METHODS ---------------------

	private JPanel getContainerPanel() {
		final JPanel containerPanel = new JPanel(new GridBagLayout());
		Border etched = BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Live Rearranger", TitledBorder.CENTER,
				TitledBorder.TOP
		);
		containerPanel.setBorder(etched);
		final GridBagConstraints scrollPaneConstraints = new GridBagConstraints();
		scrollPaneConstraints.insets = new Insets(3, 3, 3, 3);
		scrollPaneConstraints.fill = GridBagConstraints.BOTH;
		scrollPaneConstraints.gridwidth = GridBagConstraints.REMAINDER;
		scrollPaneConstraints.gridheight = GridBagConstraints.REMAINDER;
		scrollPaneConstraints.weightx = 1;
		scrollPaneConstraints.weighty = 1;
		scrollPaneConstraints.gridx = 0;
		scrollPaneConstraints.gridy = 1;

		final JScrollPane treeView = getScrollPane();
		final JComponent showTypesBox =
				new IconBox(containerPanel, scrollPaneConstraints, this) {
					@Override
					boolean getSetting() {
						return settings.isShowParameterTypes();
					}

					@Override
					void setSetting(boolean value) {
						settings.setShowParameterTypes(value);
					}

					@Override
					Icon getIcon() {
						return IconUtil.getIcon("ShowParamTypes");
					}

					@Override
					String getToolTipText() {
						return "Show parameter types";
					}

					@Override
					int getShortcut() {
						return KeyEvent.VK_T;
					}
				}.getIconBox();
		final JComponent showNamesBox =
				new IconBox(containerPanel, scrollPaneConstraints, this) {
					@Override
					boolean getSetting() {
						return settings.isShowParameterNames();
					}

					@Override
					void setSetting(boolean value) {
						settings.setShowParameterNames(value);
					}

					@Override
					Icon getIcon() {
						return IconUtil.getIcon("ShowParamNames");
					}

					@Override
					String getToolTipText() {
						return "Show parameter names";
					}

					@Override
					int getShortcut() {
						return KeyEvent.VK_N;
					}
				}.getIconBox();
		final JComponent showFieldsBox =
				new IconBox(containerPanel, scrollPaneConstraints, this) {
					@Override
					boolean getSetting() {
						return settings.isShowFields();
					}

					@Override
					void setSetting(boolean value) {
						settings.setShowFields(value);
					}

					@Override
					Icon getIcon() {
						return IconUtil.getIcon("ShowFields");
					}

					@Override
					String getToolTipText() {
						return "Show fields";
					}

					@Override
					int getShortcut() {
						return KeyEvent.VK_F;
					}
				}.getIconBox();
		final JComponent showTypeAfterMethodBox =
				new IconBox(containerPanel, scrollPaneConstraints, this) {
					@Override
					boolean getSetting() {
						return settings.isShowTypeAfterMethod();
					}

					@Override
					void setSetting(boolean value) {
						settings.setShowTypeAfterMethod(value);
					}

					@Override
					Icon getIcon() {
						return IconUtil.getIcon("ShowTypeAfterMethod");
					}

					@Override
					String getToolTipText() {
						return "Show type after method";
					}

					@Override
					int getShortcut() {
						return KeyEvent.VK_A;
					}
				}.getIconBox();
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 0;
		constraints.weighty = 0;
		constraints.insets = new Insets(5, 5, 5, 5);
		containerPanel.add(showTypesBox, constraints);
		constraints.gridx++;
		containerPanel.add(showNamesBox, constraints);
		constraints.gridx++;
		containerPanel.add(showFieldsBox, constraints);
		constraints.gridx++;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		containerPanel.add(showTypeAfterMethodBox, constraints);
		containerPanel.add(treeView, scrollPaneConstraints);
		return containerPanel;
	}

	/**
	 * build a JTree containing classes, fields and methods, in accordance with settings.
	 *
	 * @return
	 */
	@Override
	public JScrollPane getScrollPane() {
		// Create the nodes.
		PopupTree tree = treeComponent.createLiveRearrangerTree();

		/** only expand node where cursor is located.  Inspect all rows; deepest node that covers
		 * cursor location is the best to expand.  (Parent node like a class contains a method where
		 * the cursor is; we want to expand the method, not just the class.
		 */
		int expandRow = -1;
		for (int i = 0; i < tree.getRowCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getPathForRow(i).getLastPathComponent();
			if (node.getUserObject() instanceof RangeEntry) {
				RangeEntry re = (RangeEntry) node.getUserObject();
				if (re.getStart().getTextRange().getStartOffset() <= cursorOffset &&
						re.getEnd().getTextRange().getEndOffset() >= cursorOffset) {
					logger.debug(
							"node " +
									i +
									" contained cursor (offset=" +
									cursorOffset +
									"): " + re
					);
					expandRow = i;
				}
			} else {
				logger.debug("expand node candidate not RangeEntry; node=" + node);
			}
		}
		if (expandRow >= 0) {
			logger.debug("expand row " + expandRow);
			tree.expandRow(expandRow);
		}
		JScrollPane treeView = new JScrollPane(tree);
		treeView.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		treeView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		Dimension d = treeView.getPreferredSize();
		if (d.width < 400) {
			d.width = 400;
		}
		if (d.height < 300) {
			d.height = 300;
		}
		treeView.setPreferredSize(d);
		tdt = new TreeDropTarget(tree, this);
		tds = new TreeDragSource(tree, DnDConstants.ACTION_MOVE, tdt);
		return treeView;
	}

	@Override
	public void setRearrangementOccurred(boolean rearrangementOccurred) {
		this.rearrangementOccurred = rearrangementOccurred;
	}

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface ILiveRearranger ---------------------

	@Override
	public void setResultRuleInstances(List<IRuleInstance> resultRuleInstances) {
		this.resultRuleInstances = resultRuleInstances;
		treeComponent = new PopupTreeComponent(settings, resultRuleInstances, psiFileEntry);
	}

// -------------------------- OTHER METHODS --------------------------

	private FocusAdapter getFocusAdapter(final String component) {
		return new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				logger.debug(component + ": focus gained");
			}

			@Override
			public void focusLost(FocusEvent e) {
				logger.debug(component + ": focus lost, close popup & finish");
				finish();
			}

		};
	}

	public void finish() {
		logger.debug("entered finish() on thread " + Thread.currentThread().getName());
//        popup.setVisible(false);
//        popup.dispose();
//        popup.removeWindowFocusListener(windowFocusListener);
//        popup.removeWindowListener(windowAdapter);
//        popup.removeMouseListener(mouseAdapter);
//        popup.removeFocusListener(focusAdapter);
//        popup.getContentPane().removeFocusListener(focusAdapter);
		outerPanel.removeWindowFocusListener(windowFocusListener);
		if (outerPanelFocusOwner != null) {
			outerPanelFocusOwner.removeMouseListener(ma2);
		}
		outerPanel.removeWindowListener(windowAdapter);
		outerPanel.removeMouseListener(mouseAdapter);
		outerPanel.removeFocusListener(focusAdapter);
//        containerPanel.removeFocusListener(containerPanel.getFocusListeners()[0]);
		popup.hide();
//        popup.dispose();

		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyEventDispatcher);
//        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(wasLightWeight);
		if (!rearrangementOccurred) {
			logger.debug("no rearrangement occurred, not rearranging document");
			LiveRearrangerActionHandler.setInProgress(false);
			return;
		}
		logger.debug("rearranging document");
		final Runnable task = new Runnable() {

			@Override
			public void run() {
				if (document != null) {
					final Emitter e = new Emitter(psiFile, resultRuleInstances, document);
					e.emitRearrangedDocument();
				}
			}

		};
		final Application application = ApplicationManager.getApplication();

		application.runWriteAction(
				new Runnable() {

					@Override
					public void run() {
						CommandProcessor.getInstance().executeCommand(project, task, "Rearrange", null);
					}

				}
		);

		LiveRearrangerActionHandler.setInProgress(false);
		logger.debug("exit finish() on thread " + Thread.currentThread().getName());
	}

	private MouseAdapter getMouseAdapter(final String component) {
		return new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				logger.debug(
						component +
								": mouse clicked at " +
								e.getX() +
								"," +
								e.getY() +
								"; window size/position is " +
								outerPanel.getBounds()
				);
				logger.debug("close popup and finish");
				finish();
				super.mouseClicked(e);
			}

		};
	}

	private WindowAdapter getWindowAdapter(final String component) {
		return new WindowAdapter() {

			@Override
			public void windowLostFocus(WindowEvent e) {
				logger.debug(component + ": lost focus");
				logger.debug("close popup and finish");
				finish();
			}

			@Override
			public void windowClosing(WindowEvent e) {
				logger.debug(component + ": window closing");
			}

		};
	}

	private WindowFocusListener getWindowFocusListener(final String component) {
		return new WindowFocusListener() {

			@Override
			public void windowGainedFocus(WindowEvent e) {
				logger.debug(component + ": gained window focus");
			}

			@Override
			public void windowLostFocus(WindowEvent e) {
				logger.debug(component + ": lost window focus");
				logger.debug("close popup and finish");
				finish();
			}

		};
	}

// --------------------------- main() method ---------------------------

	//    class RearrangerTest
//            extends LightCodeInsightTestCase
//    {
//        private RearrangerSettings rs;
//
//        protected final void setUp() throws Exception
//        {
//            super.setUp();
//        }
//
//
//
	public static void main(String[] args) {
//        RearrangerTest t = new RearrangerTest();
//        t.testIt();
//        void testIt()
//        {
		final RearrangerSettingsImplementation settings = new RearrangerSettingsImplementation();
		IFilePopupEntry pf = new IFilePopupEntry() {

			@Override
			public String getTypeIconName() {
				return "nodes/ppFile";
			}

			@Override
			public String[] getAdditionalIconNames() {
				return null;
			}

			@Override
			public JLabel getPopupEntryText(RearrangerSettings settings) {
				return new JLabel("FileName.java");
			}

		};
		BasicConfigurator.configure();
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}

//            logger.setAdditivity(false);
//            logger.addAppender(new ConsoleAppender(new PatternLayout("[%7r] %6p - %30.30c - %m \n")));
//            logger.setLevel(Level.DEBUG);
//            logger.debug("Testing LiveRearrangerPopup");

		final JFrame frame = new JFrame("SwingApplication");
//        Window window = new Window(frame);
		JPanel panel = new JPanel();
		panel.setSize(800, 600);
		frame.setSize(800, 600);
		frame.getRootPane().add(panel);
//        window.setSize(800, 600);
//        window.getRootPane().add(panel);
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0d;
		constraints.weighty = 1.0d;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.gridx = 0;
		constraints.gridy = 0;
		LiveRearrangerPopup lrp = new LiveRearrangerPopup(
				// getProject(),
				settings, pf, frame, null,
				null
		);
//        final JPanel object = lrp.getContainerPanel();
//        frame.getContentPane().setLayout(new GridBagLayout());
//        frame.getContentPane().add(object, constraints);

		//Finish setting up the frame, and show it.
		frame.addWindowListener(
				new WindowAdapter() {

					@Override
					public void windowClosing(final WindowEvent e) {
						System.exit(0);
					}

				}
		);
//        frame.pack();
		frame.setVisible(true);
		lrp.liveRearranger();
	}

	/**
	 * Display a live rearrangement window.
	 */
	@Override
	public void liveRearranger() {
		try {
			SwingUtilities.invokeAndWait(
					new Runnable() {

						@Override
						public void run() {
							containerPanel = getContainerPanel();
							logger.debug("containerPanel.isFocusable=" + containerPanel.isFocusable());
							Border b = BorderFactory.createRaisedBevelBorder();
							containerPanel.setBorder(b);
							Dimension d = outerPanel.getSize();
							Dimension c = containerPanel.getPreferredSize();
							int x = (d.width - c.width) / 2;
							int y = (d.height - c.height) / 2;
							if (x < 0) {
								x = 0;
							}
							if (y < 0) {
								y = 0;
							}
							popup.hide(); // destroy initial (interim) popup
							popup = PopupFactory.getSharedInstance().getPopup(outerPanel, containerPanel, x, y);
//        popup = new JFrame();
//                            final Container contentPane = popup.getContentPane();
//                            contentPane.remove(0);
//                            contentPane.add(containerPanel);
//                            contentPane.validate();
//                            popup.setLocation(x, y);
							logger.debug("setCursor (original)" + oldCursor + " on " + outerPanel);
							outerPanel.setCursor(oldCursor);
//                            logger.debug("set default cursor on " + popup);
//                            popup.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
//                            popup.repaint();
							logger.debug("outerPanel size=" + d + ", containerPanel preferred size=" + c);
							logger.debug("Constructing Popup at x,y=" + x + "," + y);
							LiveRearrangerActionHandler.setInProgress(true);

//                            popup.addWindowFocusListener(windowFocusListener);
//                            popup.addWindowListener(windowAdapter);
//                            popup.addMouseListener(mouseAdapter);
//                            popup.addFocusListener(focusAdapter);
//                            contentPane.addFocusListener(focusAdapter);
							outerPanel.addWindowFocusListener(
									windowFocusListener = getWindowFocusListener("outerPanel")
							);
							outerPanel.addWindowListener(windowAdapter = getWindowAdapter("outerPanel"));
							outerPanel.addMouseListener(mouseAdapter = getMouseAdapter("outerPanel"));
							ma2 = getMouseAdapter("focus owner");
							outerPanelFocusOwner = outerPanel.getFocusOwner();
							if (outerPanelFocusOwner != null) {
								outerPanelFocusOwner.addMouseListener(ma2);
							}
							outerPanel.addFocusListener(focusAdapter = getFocusAdapter("outerPanel"));
							containerPanel.addFocusListener(getFocusAdapter("containerPanel"));

							keyEventDispatcher = new KeyEventDispatcher() {

								@Override
								public boolean dispatchKeyEvent(KeyEvent e) {
									logger.debug(
											"key event dispatcher: outerPanel isFocused=" +
													outerPanel.isFocused() +
													", rearrangementOccurred=" +
													rearrangementOccurred +
													" keyEvent=" +
													e
									);
									if (e.getID() == KeyEvent.KEY_RELEASED &&
											(rearrangementOccurred || sawKeyPressed)) {
										logger.debug("key event dispatcher: disposing popup, saw keyEvent " + e);
										finish();
									}
									if (e.getID() == KeyEvent.KEY_PRESSED) {
										sawKeyPressed = true;
									}
									return true;
								}

							};
							KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(
									keyEventDispatcher
							);
//        wasLightWeight = ToolTipManager.sharedInstance().isLightWeightPopupEnabled();
//        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
							popup.show();
//        popup.setVisible(true);
						}

					}
			);
		} catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		} catch (InvocationTargetException ite) {
			throw new RuntimeException(ite);
		}
		logger.debug("exit liveRearranger");
	}

}

//
//}
