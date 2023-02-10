/********************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.core.ui.internal.layout;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


/**
 *
 */
public class GridBagConstraints {

  /**
   * 
   */
  private static final String DATA_KEY = GridBag.class.getName();

  /**
   * 
   */
  private static final GridBagConstraints DEFAULT = new GridBagConstraints();

  public static final int RELATIVE = -1;

  public static final int REMAINDER = -1;

  /**
   * Specify the grid coordinate where the component will be placed, starting with 0 and defaulting to
   * {@link #RELATIVE}, it is recommended to always set a value to have more predictable layouts that are independent of
   * the order components are added to the parent composite.
   */
  public final Point grid = new Point(RELATIVE, RELATIVE);

  /**
   * Specify how much columns/rows the component spans (default to 1), specify {@link #REMAINDER} to take all remaining
   * ones or {@link #RELATIVE}, it is recommended to always specify a value to get more predictable layouts
   */
  public final Point span = new Point(1, 1);

  /**
   * The weighting factor for the column this component is placed with, at least one control needs to define a positive
   * weight factor for each col/row, usually one want to take values in the range 0...100 to ease recognition by a human
   * being in percentages.
   */
  public final Point weight = new Point(0, 0);

  /**
   * Height hint used to determine the preferred size of the component
   */
  public int hHint = SWT.DEFAULT;

  /**
   * Width hint used to determine the preferred size of the component
   */
  public int wHint = SWT.DEFAULT;

  /**
   * Controls how/if the control should fill:
   * <ul>
   * <li>{@link SWT#NONE} - the component does not fill (default)</li>
   * <li>{@link SWT#HORIZONTAL} - the component is filled horizontal</li>
   * <li>{@link SWT#VERTICAL} - the component fills vertical</li>
   * <li>{@link SWT#ALL} - the component is filled in both directions (TODO: new swt constant?)</li>
   * </ul>
   */
  public int fill = SWT.NONE;

  static GridBag getGridBag(Composite composite, boolean changed) {
    if(!changed) {
      Object data = composite.getData(DATA_KEY);
      if(data instanceof GridBag) {
        return (GridBag) data;
      }
    }
    Control[] children = composite.getChildren();
    GridBagConstraints[] contrains = new GridBagConstraints[children.length];
    GridBag grid = new GridBag();
    for(int i = 0; i < children.length; i++ ) {
      Control control = children[i];
      GridBagConstraints c = contrains[i] = getConstraints(control);
      int x = c.grid.x;
      int y = c.grid.y;
      if(x == GridBagConstraints.RELATIVE) {
        //TODO
      }
      if(y == GridBagConstraints.RELATIVE) {
        //TODO
      }
      //TODO calculate the span if it is RELATIVE or REMAINDER!
      //For this to work, we maybe need to move this to the second loop!
      int spanx = c.span.x;
      int spany = c.span.y;
      int xd = x + spanx;
      if(xd > grid.dimension.x) {
        grid.dimension.x = xd;
      }
      int yd = y + spany;
      if(yd > grid.dimension.y) {
        grid.dimension.y = yd;
      }
      grid.positionMap.put(control, new Point(x, y));
      grid.constraintMap.put(control, c);
    }
    grid.controls = new Control[grid.dimension.x][grid.dimension.y];
    grid.cols = new int[grid.dimension.x];
    grid.rows = new int[grid.dimension.y];
    grid.rowWeights = new double[grid.rows.length];
    grid.colWeights = new double[grid.cols.length];
    for(int i = 0; i < children.length; i++ ) {
      Control control = children[i];
      GridBagConstraints c = contrains[i];
      Point size = control.computeSize(c.wHint, c.hHint, changed);
      grid.sizeMap.put(control, size);
      Point pos = grid.positionMap.get(control);
      int x = pos.x;
      int y = pos.y;
      int w = size.x / c.span.x;
      int h = size.y / c.span.y;
      grid.controls[x][y] = control;
      for(int gi = x; gi < x + c.span.x; gi++ ) {
        if(w > grid.cols[gi]) {
          grid.cols[gi] = w;
        }
      }
      for(int gi = y; gi < y + c.span.y; gi++ ) {
        if(h > grid.rows[gi]) {
          grid.rows[gi] = h;
        }
      }
      double rw = (double) c.weight.x / c.span.x;
      double cw = (double) c.weight.y / c.span.y;
      for(int gi = x; gi < x + c.span.x; gi++ ) {
        if(rw > grid.colWeights[gi]) {
          grid.colWeights[gi] = rw;
        }
      }
      for(int gi = y; gi < y + c.span.y; gi++ ) {
        if(cw > grid.rowWeights[gi]) {
          grid.rowWeights[gi] = cw;
        }
      }
    }
    composite.setData(DATA_KEY, grid);
    return grid;
  }

  static GridBagConstraints getConstraints(Control control) {
    Object layoutData = control.getLayoutData();
    if(layoutData instanceof GridBagConstraints) {
      return (GridBagConstraints) layoutData;
    }
    return DEFAULT;
  }

  static final class GridBag {

    public int[] cols;

    int[] rows;

    double[] rowWeights;

    double[] colWeights;

    Point dimension = new Point(0, 0);

    Map<Control, Point> positionMap = new HashMap<>();

    Map<Control, GridBagConstraints> constraintMap = new HashMap<>();

    Map<Control, Point> sizeMap = new HashMap<>();

    Control[][] controls;

    Point getSize(Control control) {
      return sizeMap.get(control);
    }

    int getColumnWidth(int column) {
      return cols[column];
    }

    int getRowHeight(int row) {
      return rows[row];
    }

    public int[] getRowHeights(int height) {
      if(rows.length == 0) {
        return new int[0];
      }
      int[] is = new int[rows.length];
      double weights = 0;
      for(int i = 0; i < is.length; i++ ) {
        weights += rowWeights[i];
      }
      double dx = height / weights;
      int sum = 0;
      for(int i = 0; i < is.length; i++ ) {
        sum += is[i] += Math.rint(dx * rowWeights[i]);
      }
      //any rounding error go to the last column
      is[is.length - 1] += height - sum;
      return is;
    }

    public int[] getColumnWidths(int width) {
      if(cols.length == 0) {
        return new int[0];
      }
      int[] is = new int[cols.length];
      double weights = 0;
      for(int i = 0; i < is.length; i++ ) {
        weights += colWeights[i];
      }
      double dx = width / weights;
      int sum = 0;
      for(int i = 0; i < is.length; i++ ) {
        sum += is[i] += Math.rint(dx * colWeights[i]);
      }
      //any rounding error go to the last column
      is[is.length - 1] += width - sum;
      return is;
    }

  }
}
