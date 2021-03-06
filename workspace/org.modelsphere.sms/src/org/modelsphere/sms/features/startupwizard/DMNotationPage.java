/*************************************************************************

Copyright (C) 2009 Grandite

This file is part of Open ModelSphere.

Open ModelSphere is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
or see http://www.gnu.org/licenses/.

You can reach Grandite at: 

20-1220 Lebourgneuf Blvd.
Quebec, QC
Canada  G2K 2G4

or

open-modelsphere@grandite.com

 **********************************************************************/
package org.modelsphere.sms.features.startupwizard;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.modelsphere.jack.awt.ThinBevelBorder;
import org.modelsphere.jack.baseDb.db.DbEnumeration;
import org.modelsphere.jack.baseDb.db.DbException;
import org.modelsphere.jack.baseDb.db.DbProject;
import org.modelsphere.jack.gui.wizard2.AbstractPage;
import org.modelsphere.jack.gui.wizard2.SectionHeader;
import org.modelsphere.jack.util.CollationComparator;
import org.modelsphere.jack.util.SrSort;
import org.modelsphere.sms.db.DbSMSNotation;
import org.modelsphere.sms.db.DbSMSStyle;
import org.modelsphere.sms.db.util.DbInitialization;
import org.modelsphere.sms.features.international.LocaleMgr;
import org.modelsphere.sms.or.db.DbORNotation;

@SuppressWarnings("serial")
class DMNotationPage extends AbstractPage {
    private JList notationList = new JList();
    private JLabel previewLabel = new JLabel();

    private StartupWizardModel model = null;

    DMNotationPage(StartupWizardModel model) {
        super(new GridBagLayout());
        this.model = model;
        init();
    }

    private void init() {
        JScrollPane scrollPane = new JScrollPane(notationList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        notationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        SectionHeader notationHeader = new SectionHeader(StartupWizard.kChoose_a_notation);
        SectionHeader previewHeader = new SectionHeader(StartupWizard.kPreview);

        previewLabel.setBorder(new ThinBevelBorder(ThinBevelBorder.LOWERED));

        add(notationHeader, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        add(scrollPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(6, 24, 0, 0), 0, 0));
        add(previewHeader, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(12, 0, 0, 0), 0, 0));
        add(previewLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(6, 24, 0, 0), 0, 0));
        add(Box.createVerticalGlue(), new GridBagConstraints(0, 4, 1, 1, 0.0, 0.5,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        notationList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                String notationStrSelected = null;

                notationStrSelected = (String) notationList.getSelectedValue();
                if (notationStrSelected == null) {
                    notationStrSelected = model.getDmNotation();
                    model.setDmNotation(null);
                    model.setErNotation(null);
                    model.setOrNotation(null);
                    model.setChosenStyle(null);
                    model.setNotation(null);
                    previewLabel.setIcon(new ImageIcon(LocaleMgr.screen
                            .getImage("notationBlankPicture")));//NOT LOCALIZABLE 
                } else {
                    Integer orNotationID = null;
                    if (model.getDataModelType() == StartupWizardModel.DATAMODEL_TYPE_ENTITY_RELATION) {
                        model.setErNotation(notationStrSelected);
                        model.setOrNotation(null);
                    } else {
                        model.setOrNotation(notationStrSelected);
                        model.setErNotation(null);
                    }
                    model.setDmNotation(notationStrSelected);
                    try {
                        DbProject project = model.getChosenProject();
                        project.getDb().beginReadTrans();
                        DbSMSNotation notation = (DbSMSNotation) project.findComponentByName(
                                DbORNotation.metaClass, notationStrSelected);
                        orNotationID = ((DbSMSNotation) notation).getNotationID();
                        model.setNotation(notation);
                        DbSMSStyle style = notation.getDefaultStyle();
                        model.setChosenStyle(style);
                        project.getDb().commitTrans();

                    } catch (DbException ex) {
                    }
                    if (orNotationID.intValue() >= DbInitialization.NEW_NOTATION_START_ID) {
                        previewLabel.setIcon(new ImageIcon(LocaleMgr.screen
                                .getImage("notationBlankPicture")));//NOT LOCALIZABLE 
                    } else {
                        String notationKeyStr = "notation"; //NOT LOCALIZABLE 
                        notationKeyStr = notationKeyStr.concat(orNotationID.toString());
                        previewLabel.setIcon(new ImageIcon(LocaleMgr.screen
                                .getImage(notationKeyStr)));
                    }
                }
            }
        });

    }

    JList getNotationList() {
        return notationList;
    }

    @Override
    public void load() throws DbException {
        initNotationList();
    }

    private void initNotationList() throws DbException {
        ArrayList<String> notationListOut = new ArrayList<String>();
        DbProject project = model.getChosenProject();

        project.getDb().beginReadTrans();
        DbEnumeration dbEnum = null;
        dbEnum = project.getComponents().elements(DbORNotation.metaClass);
        while (dbEnum.hasMoreElements()) {
            Object notation = dbEnum.nextElement();
            String notationName = ((DbSMSNotation) notation).getName();
            Integer mode = ((DbSMSNotation) notation).getNotationMode();
            if (mode != null) {
                if (((mode.intValue() == DbSMSNotation.OR_MODE) && (model.getDataModelType() == StartupWizardModel.DATAMODEL_TYPE_ENTITY_RELATION))
                        || ((mode.intValue() == DbSMSNotation.ER_MODE) && (model.getDataModelType() != StartupWizardModel.DATAMODEL_TYPE_ENTITY_RELATION))) {
                    continue;
                }
            }

            notationListOut.add(notationName);
        }
        dbEnum.close();

        Object[] array = notationListOut.toArray();
        CollationComparator comparator = new CollationComparator();
        SrSort.sortArray(array, array.length, comparator);
        notationList.setListData(array);

        notationList.setSelectedValue((Object) model.getDmNotation(), true);

        project.getDb().commitTrans();
    }

    @Override
    public void save() throws DbException {
    }

    @Override
    public void rollback() throws DbException {
    }

    @Override
    public String getTitle() {
        return StartupWizard.kNotation;
    }

}
