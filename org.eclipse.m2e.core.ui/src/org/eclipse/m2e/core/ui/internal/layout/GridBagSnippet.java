/********************************************************************************
 * Copyright (c) 2023, 2023 christoph and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   christoph - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.core.ui.internal.layout;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;


/**
 * @author christoph
 */
public class GridBagSnippet {
  public static void main(String[] args) {
    Display display = new Display();
    Shell shell = new Shell(display);
    shell.setText("GridBagLayout Demo");
    shell.setLayout(new GridBagLayout(shell));
    Table table = createTable(shell);
    GridBagConstraints tableConstraints = new GridBagConstraints();
    //The table is placed at position 0, 0
    tableConstraints.grid.x = 0;
    tableConstraints.grid.y = 0;
    //... it always uses all space
    tableConstraints.fill = SWT.ALL;
    //... and 80 % of vertical space
    tableConstraints.weight.y = 80;
    // ... and spawns 2 columns
    tableConstraints.span.x = 2;
    table.setLayoutData(tableConstraints);
    Control text = createTextArea(shell, table);
    GridBagConstraints textConstraints = new GridBagConstraints();
    //The text area  is placed at position 0, 1 just below the table
    textConstraints.grid.x = 0;
    textConstraints.grid.y = 1;
    textConstraints.fill = SWT.ALL;
    //it takes one third
    textConstraints.weight.x = 30;
    textConstraints.weight.y = 20;
    text.setLayoutData(textConstraints);
    Label label = createLabel(shell, table);
    //the label is placed at position 1, 1
    GridBagConstraints labelConstraints = new GridBagConstraints();
    labelConstraints.grid.x = 1;
    labelConstraints.grid.y = 1;
    labelConstraints.weight.x = 30;
    labelConstraints.fill = SWT.ALL;
    label.setLayoutData(labelConstraints);
    Control buttons = createButtons(shell);
    GridBagConstraints buttonConstraints = new GridBagConstraints();
    buttonConstraints.grid.x = 2;
    buttonConstraints.grid.y = 0;
    buttonConstraints.weight.x = 30;
    buttonConstraints.span.y = 2;
    buttons.setLayoutData(buttonConstraints);
    shell.open();
    while(!shell.isDisposed()) {
      if(!display.readAndDispatch()) {
        display.sleep();
      }
    }
    display.dispose();
  }

  /**
   * @param shell
   * @return
   */
  private static Control createButtons(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(1, true));
    for(int i = 0; i < 10; i++ ) {
      new Button(composite, SWT.PUSH).setText("Button " + i);
    }
    return composite;
  }

  private static Label createLabel(Composite parent, Table table) {
    Label label = new Label(parent, SWT.CENTER);
    label.setText("");
    label.setForeground(label.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    table.addSelectionListener(new SelectionListener() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        int index = table.getSelectionIndex();
        label.setText("You selected " + index);
        if(index % 2 == 0) {
          label.setBackground(e.display.getSystemColor(SWT.COLOR_BLUE));
        } else {
          label.setBackground(e.display.getSystemColor(SWT.COLOR_RED));
        }
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {

      }
    });
    return label;
  }

  private static Control createTextArea(Composite parent, Table table) {
    ScrolledComposite composite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
    composite.setLayout(new FillLayout());
    Text text = new Text(composite, SWT.MULTI);
    table.addSelectionListener(new SelectionListener() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        StringBuilder sb = new StringBuilder();
        for(TableItem item : table.getSelection()) {
          for(int i = 0; i < table.getColumnCount(); i++ ) {
            sb.append(item.getText(i));
            sb.append(" | ");
          }
          sb.append(Text.DELIMITER);
        }
        text.setText(sb.toString());
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {

      }
    });
    text.setText("Select a table item!");
    composite.setContent(text);
    composite.setMinSize(800, 800);
    composite.setExpandHorizontal(true);
    composite.setExpandVertical(true);
    return composite;
  }

  //From Snippet38
  public static Table createTable(Composite parent) {
    Table table = new Table(parent, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
    table.setLinesVisible(true);
    table.setHeaderVisible(true);
    String[] titles = {" ", "C", "!", "Description", "Resource", "In Folder", "Location"};
    for(String title : titles) {
      TableColumn column = new TableColumn(table, SWT.NONE);
      column.setText(title);
    }
    int count = 128;
    for(int i = 0; i < count; i++ ) {
      TableItem item = new TableItem(table, SWT.NONE);
      item.setText(0, "x");
      item.setText(1, "y");
      item.setText(2, "!");
      item.setText(3, "this stuff behaves the way I expect");
      item.setText(4, "almost everywhere");
      item.setText(5, "some.folder");
      item.setText(6, "line " + i + " in nowhere");
    }
    for(int i = 0; i < titles.length; i++ ) {
      table.getColumn(i).pack();
    }
    return table;
  }
}
