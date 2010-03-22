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

package org.apache.harmony.jretools.policytool.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * The abstraction of a panel which can list entities and provide GUI components to offer and handle certain actions on the entities.<br>
 * The entities are listed in a listbox.
 * @param <EntityType> type of the entities listed on and edited by this panel
 */
public class ListAndEditPanel< EntityType > extends JPanel implements ActionListener {

    /** Preferred dimension of the component listing the entities. */
    private static final Dimension PREFERRED_LIST_COMPONENT_SIZE = new Dimension( 310, 150 );

    /** Model of the list component displaying the entities. */
    private final DefaultListModel listModel           = new DefaultListModel();
    /** The component to list he entities. */
    private final JList            entityListComponent = new JList( listModel );

    /** Add new entity button.             */
    private final JButton          addButton           = new JButton();
    /** Edit selected entity button.       */
    private final JButton          editButton          = new JButton();
    /** Remove selected entity button.     */
    private final JButton          removeButton        = new JButton();

    /** Reference to the list whose elements are to be listed and edited, and where to put new entities. */
    private final List< ? >        entityList;

    /** Reference to the base form dialog factory. */
    private final LAEFormDialogFactory< EntityType > baseFormDialogFactory;

    /**
     * Can be used to filter the input entity list, hide elements from displaying.
     * @param <EntityType> type of the entities filtered by this filter
     */
    public interface Filter< EntityType > {
        /**
         * Tells whether to include an entity in the list-and-edit process
         * @param entity entity to be tested
         * @return true if the entity should be listed and edited; false if it should be and excluded and hid
         */
        boolean includeEntity( final EntityType entity );
    }

    /**
     * Factory instance to be used to acuire a base form dialog which will handle the add/edit action of the selected entity.
     * @param <EntityType> type of the entities which is (listed and edited and) passed by by this ListAndEditPanel
     */
    public interface LAEFormDialogFactory< EntityType > {
        /**
         * Creates a <code>LAEFormDialog<code> which will handle the add/edit action of the passed selected entity.
         * @param selectedEntity selected entity to be edited or null if a new one should be created and added
         * @return a reference to the created BaseFormDialog
         */
        LAEFormDialog createFactoryForAddOrEdit( final EntityType selectedEntity );
    }


    /**
     * Creates a new ListAndEditPanel.<br>
     * Sets a BorderLayout for ourselves.
     * @param panelTitle title of the list and edit panel
     * @param entityName name of the listed and edited entity (this will be displayed on the buttons)
     * @param entityList reference to the list whose elements are to be listed and edited, and where to put new entities
     * @param baseFormDialogFactory reference to a base form dialog factory
     */
    public ListAndEditPanel( final String panelTitle, final String entityName, final List< EntityType > entityList, final LAEFormDialogFactory< EntityType > baseFormDialogFactory  ) {
        this( panelTitle, entityName, entityList, null, baseFormDialogFactory );
    }

    /**
     * Creates a new ListAndEditPanel.<br>
     * Sets a BorderLayout for ourselves.
     * @param panelTitle title of the list and edit panel
     * @param entityName name of the listed and edited entity (this will be displayed on the buttons)
     * @param entityList reference to the list whose elements are to be listed and edited, and where to put new entities
     * @param entityFilter filter to be used when listing the entities
     * @param baseFormDialogFactory reference to a base form dialog factory
     */
    public ListAndEditPanel( final String panelTitle, final String entityName, final List< EntityType > entityList, final Filter< EntityType > entityFilter, final LAEFormDialogFactory< EntityType > baseFormDialogFactory ) {
        super( new BorderLayout() );

        this.entityList            = entityList;
        this.baseFormDialogFactory = baseFormDialogFactory;

        for ( final EntityType entity : entityList )
            if ( entityFilter == null || entityFilter.includeEntity( entity ) )
                listModel.addElement( entity );

        buildGUI( panelTitle, entityName );
    }

    /**
     * Builds the graphical user interface of the panel.
     * @param panelTitle title of the list and edit panel
     * @param entityName name of the listed and edited entity (this will be displayed on the buttons)
     */
    private void buildGUI( final String panelTitle, final String entityName ) {
        final JPanel buttonsPanel = new JPanel();

        addButton   .setText          ( "Add " + entityName                );
        addButton   .setMnemonic      ( addButton   .getText().charAt( 0 ) );
        addButton   .addActionListener( this                               );
        buttonsPanel.add( addButton );

        editButton  .setText          ( "Edit " + entityName               );
        editButton  .setMnemonic      ( editButton  .getText().charAt( 0 ) );
        editButton  .addActionListener( this                               );
        buttonsPanel.add( editButton );

        removeButton.setText          ( "Remove " + entityName             );
        removeButton.setMnemonic      ( removeButton.getText().charAt( 0 ) );
        removeButton.addActionListener( this                               );
        buttonsPanel.add( removeButton );

        add( buttonsPanel, BorderLayout.NORTH );

        final JScrollPane scrollPane =  new JScrollPane( entityListComponent );
        scrollPane.setPreferredSize( PREFERRED_LIST_COMPONENT_SIZE );
        add( scrollPane, BorderLayout.CENTER );

        setBorder( BorderFactory.createTitledBorder( panelTitle ) );
    }

    /**
     * Overrides default mnemonic keys for the add, edit and remove buttons.<br>
     * If null value is passed as a mnemonic, the mnemonic for that button will not be changed.
     * @param addButtonMnemonic new mnemonic for the add button
     * @param editButtonMnemonic new mnemonic for the edit button
     * @param removeButtonMnemonic new mnemonic for the remove button
     */
    public void overrideMnemonics( final Character addButtonMnemonic, final Character editButtonMnemonic, final Character removeButtonMnemonic ) {
        if ( addButtonMnemonic    != null )
            addButton   .setMnemonic( addButtonMnemonic    );

        if ( editButtonMnemonic   != null )
            editButton  .setMnemonic( editButtonMnemonic   );

        if ( removeButtonMnemonic != null )
            removeButton.setMnemonic( removeButtonMnemonic );
    }

    /**
     * Handles the action events of the buttons for adding new, editing and removing entities.
     * @param ae details of the action event
     */
    @SuppressWarnings("unchecked")
    public void actionPerformed( final ActionEvent ae ) {
        if ( ae.getSource() == addButton ) {
            final LAEFormDialog laeFormDialog = baseFormDialogFactory.createFactoryForAddOrEdit( null );
            laeFormDialog.setVisualizationListForLAE( entityListComponent, listModel );
            laeFormDialog.setVisible( true );
        } else if ( ae.getSource() == editButton ) {
            final EntityType selectedEntity = (EntityType) entityListComponent.getSelectedValue();
            if ( selectedEntity != null ) {
                final LAEFormDialog laeFormDialog = baseFormDialogFactory.createFactoryForAddOrEdit( selectedEntity );
                laeFormDialog.setVisualizationListForLAE( entityListComponent, listModel );
                laeFormDialog.setVisible( true );
            }
        } else if ( ae.getSource() == removeButton ) {
            for ( final Object selectedEntityObject : entityListComponent.getSelectedValues() ) {
                listModel .removeElement( selectedEntityObject );
                entityList.remove       ( selectedEntityObject );
            }
        }
    }

}
