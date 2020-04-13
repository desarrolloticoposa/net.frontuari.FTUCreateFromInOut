package net.frontuari.ftucfio.webui.apps.form;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.apps.form.WCreateFromShipmentUI;
import org.adempiere.webui.apps.form.WCreateFromWindow;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WLocatorEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WStringEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.apps.IStatusBar;
import org.compiere.grid.CreateFrom;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.GridTab;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MLocator;
import org.compiere.model.MLocatorLookup;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRMA;
import org.compiere.model.MRMALine;
import org.compiere.model.MWarehouse;
import org.compiere.model.X_M_InOut;

import static org.compiere.model.SystemIDs.*;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Space;
import org.zkoss.zul.Vlayout;

public class WFTUCreateFromShipmentUI extends CreateFrom implements EventListener<Event>, ValueChangeListener
{
	
	private WCreateFromWindow window;
	
	public WFTUCreateFromShipmentUI(GridTab tab) 
	{
		super(tab);
		log.info(getGridTab().toString());
		
		window = new WCreateFromWindow(this, getGridTab().getWindowNo());
		
		p_WindowNo = getGridTab().getWindowNo();

		try
		{
			if (!dynInit())
				return;
			zkInit();
			setInitOK(true);
		}
		catch(Exception e)
		{
			log.log(Level.SEVERE, "", e);
			setInitOK(false);
			throw new AdempiereException(e.getMessage());
		}
		AEnv.showWindow(window);
	}
	
	/** Window No               */
	private int p_WindowNo;

	/**	Logger			*/
	private final static CLogger log = CLogger.getCLogger(WCreateFromShipmentUI.class);
		
	protected Label bPartnerLabel = new Label();
	protected WEditor bPartnerField;
	
	protected Label orderLabel = new Label();
	protected Listbox orderField = ListboxFactory.newDropdownListbox();

    /** Label for the rma selection */
    protected Label rmaLabel = new Label();
    /** Combo box for selecting RMA document */
    protected Listbox rmaField = ListboxFactory.newDropdownListbox();
	
    protected Label invoiceLabel = new Label();
    protected Listbox invoiceField = ListboxFactory.newDropdownListbox();
	protected Checkbox sameWarehouseCb = new Checkbox();
	protected Label locatorLabel = new Label();
	protected WLocatorEditor locatorField = new WLocatorEditor();
	protected Label upcLabel = new Label();
	protected WStringEditor upcField = new WStringEditor();

	private Grid parameterStdLayout;

	private int noOfParameterColumn;
    
	/**
	 *  Dynamic Init
	 *  @throws Exception if Lookups cannot be initialized
	 *  @return true if initialized
	 */
	public boolean dynInit() throws Exception
	{
		log.config("");
		
		window.setTitle(Msg.getElement(Env.getCtx(), "CreateFrom"));

		sameWarehouseCb.setSelected(true);
		sameWarehouseCb.addActionListener(this);
		//  load Locator
		MLocatorLookup locator = new MLocatorLookup(Env.getCtx(), p_WindowNo);
		locatorField = new WLocatorEditor ("M_Locator_ID", true, false, true, locator, p_WindowNo);

		initBPartner(false);
		bPartnerField.addValueChangeListener(this);
		locatorLabel.setMandatory(true);

		upcField = new WStringEditor ("UPC", false, false, true, 10, 30, null, null);
		upcField.getComponent().addEventListener(Events.ON_CHANGE, this);

		return true;
	}   //  dynInit
	
	protected void zkInit() throws Exception
	{
    	boolean isRMAWindow = ((getGridTab().getAD_Window_ID() == WINDOW_RETURNTOVENDOR) || (getGridTab().getAD_Window_ID() == WINDOW_CUSTOMERRETURN)); 

    	bPartnerLabel.setText(Msg.getElement(Env.getCtx(), "C_BPartner_ID"));
		orderLabel.setText(Msg.getElement(Env.getCtx(), "C_Order_ID", false));
		invoiceLabel.setText(Msg.getElement(Env.getCtx(), "C_Invoice_ID", false));
        rmaLabel.setText(Msg.translate(Env.getCtx(), "M_RMA_ID"));
		locatorLabel.setText(Msg.translate(Env.getCtx(), "M_Locator_ID"));
        sameWarehouseCb.setText(Msg.getMsg(Env.getCtx(), "FromSameWarehouseOnly", true));
        sameWarehouseCb.setTooltiptext(Msg.getMsg(Env.getCtx(), "FromSameWarehouseOnly", false));
        upcLabel.setText(Msg.getElement(Env.getCtx(), "UPC", false));

		Vlayout vlayout = new Vlayout();
		ZKUpdateUtil.setVflex(vlayout, "min");
		ZKUpdateUtil.setWidth(vlayout, "100%");
    	Panel parameterPanel = window.getParameterPanel();
		parameterPanel.appendChild(vlayout);
		
		parameterStdLayout = GridFactory.newGridLayout();
    	vlayout.appendChild(parameterStdLayout);
    	ZKUpdateUtil.setVflex(vlayout, "parameterStdLayout");
    	
    	setupColumns(parameterStdLayout);
		
		Rows rows = (Rows) parameterStdLayout.newRows();
		Row row = rows.newRow();
		row.appendChild(bPartnerLabel.rightAlign());
		if (bPartnerField != null) {
			row.appendChild(bPartnerField.getComponent());
			bPartnerField.fillHorizontal();
		}
    	if (! isRMAWindow) {
    		row.appendChild(orderLabel.rightAlign());
    		row.appendChild(orderField);
    		ZKUpdateUtil.setHflex(orderField, "1");
    	}
		
		row = rows.newRow();
		row.appendChild(locatorLabel.rightAlign());
		row.appendChild(locatorField.getComponent());
    	if (! isRMAWindow) {
    		row.appendChild(invoiceLabel.rightAlign());
    		row.appendChild(invoiceField);
    		ZKUpdateUtil.setHflex(invoiceField, "1");
    	}
        
		row = rows.newRow();
		row.appendChild(new Space());
		row.appendChild(sameWarehouseCb);
		
		row = rows.newRow();
		row.appendChild(upcLabel.rightAlign());
		row.appendChild(upcField.getComponent());
		ZKUpdateUtil.setHflex(upcField.getComponent(), "1");
    	if (isRMAWindow) {
            // Add RMA document selection to panel
            row.appendChild(rmaLabel.rightAlign());
            row.appendChild(rmaField);
            ZKUpdateUtil.setHflex(rmaField, "1");
    	}
    	
    	if (ClientInfo.isMobile()) {    		
    		if (noOfParameterColumn == 2)
				LayoutUtils.compactTo(parameterStdLayout, 2);		
			ClientInfo.onClientInfo(window, this::onClientInfo);
		}
	}

	private boolean 	m_actionActive = false;

	private Object m_invoice;

	private int defaultLocator_ID;
	
	/**
	 *  Action Listener
	 *  @param e event
	 * @throws Exception 
	 */
	public void onEvent(Event e) throws Exception
	{
		if (m_actionActive)
			return;
		m_actionActive = true;
		/*
		//  Order
		if (e.getTarget().equals(orderField))
		{
			ListItem li = orderField.getSelectedItem();
			int C_Order_ID = 0;
			if (li != null && li.getValue() != null)
				C_Order_ID = ((Integer) li.getValue()).intValue();
			//  set Invoice, RMA and Shipment to Null
			rmaField.setSelectedIndex(-1);
			//shipmentField.setSelectedIndex(-1);
			loadOrder(C_Order_ID, true);
		}
		//  Shipment
		else if (e.getTarget().equals(invoiceField))
		{
			ListItem li = shipmentField.getSelectedItem();
			int M_InOut_ID = 0;
			if (li != null && li.getValue() != null)
				M_InOut_ID = ((Integer) li.getValue()).intValue();
			//  set Order, RMA and Invoice to Null
			orderField.setSelectedIndex(-1);
			rmaField.setSelectedIndex(-1);
			loadShipment(M_InOut_ID);
		}
		//  RMA
		else if (e.getTarget().equals(rmaField))
		{
			ListItem li = rmaField.getSelectedItem();
		    int M_RMA_ID = 0;
		    if (li != null && li.getValue() != null)
		        M_RMA_ID = ((Integer) li.getValue()).intValue();
		    //  set Order and Invoice to Null
		    orderField.setSelectedIndex(-1);
		    //shipmentField.setSelectedIndex(-1);
		    loadRMA(M_RMA_ID);
		}
		m_actionActive = false;
		*/
		
		//  Order
		if (e.getTarget().equals(orderField))
		{
			KeyNamePair pp = orderField.getSelectedItem().toKeyNamePair();
			if (pp == null || pp.getKey() == 0)
				;
			else
			{
				int C_Order_ID = pp.getKey();
				//  set Invoice and Shipment to Null
				invoiceField.setSelectedIndex(-1);
                rmaField.setSelectedIndex(-1);
				loadOrder(C_Order_ID, false, locatorField.getValue()!=null?((Integer)locatorField.getValue()).intValue():0);
			}
		}
		//  Invoice
		else if (e.getTarget().equals(invoiceField))
		{
			KeyNamePair pp = invoiceField.getSelectedItem().toKeyNamePair();
			if (pp == null || pp.getKey() == 0)
				;
			else
			{
				int C_Invoice_ID = pp.getKey();
				//  set Order and Shipment to Null
				orderField.setSelectedIndex(-1);
                rmaField.setSelectedIndex(-1);
				loadInvoice(C_Invoice_ID, locatorField.getValue()!=null?((Integer)locatorField.getValue()).intValue():0);
			}
		}
		// RMA
        else if (e.getTarget().equals(rmaField))
        {
            KeyNamePair pp = rmaField.getSelectedItem().toKeyNamePair();
            if (pp == null || pp.getKey() == 0)
                ;
            else
            {
                int M_RMA_ID = pp.getKey();
                //  set Order and Shipment to Null
                orderField.setSelectedIndex(-1);
                invoiceField.setSelectedIndex(-1);
                loadRMA(M_RMA_ID, locatorField.getValue()!=null?((Integer)locatorField.getValue()).intValue():0);
            }
        }
		//sameWarehouseCb
        else if (e.getTarget().equals(sameWarehouseCb))
        {
        	int bpId = bPartnerField.getValue() == null?0:((Integer)bPartnerField.getValue()).intValue();
        	initBPOrderDetails(bpId, false);
        }	
		else if (e.getTarget().equals(upcField.getComponent()))
		{
			checkProductUsingUPC();
		}
		
		m_actionActive = false;
	}
	
	/**
	 * Checks the UPC value and checks if the UPC matches any of the products in the
	 * list.
	 */
	private void checkProductUsingUPC()
	{
		String upc = upcField.getDisplay();
		//DefaultTableModel model = (DefaultTableModel) dialog.getMiniTable().getModel();
		ListModelTable model = (ListModelTable) window.getWListbox().getModel();
		
		// Lookup UPC
		List<MProduct> products = MProduct.getByUPC(Env.getCtx(), upc, null);
		for (MProduct product : products)
		{
			int row = findProductRow(product.get_ID());
			if (row >= 0)
			{
				BigDecimal qty = (BigDecimal)model.getValueAt(row, 1);
				model.setValueAt(qty, row, 1);
				model.setValueAt(Boolean.TRUE, row, 0);
				model.updateComponent(row, row);
			}
		}
		upcField.setValue("");
	}

	/**
	 * Finds the row where a given product is. If the product is not found
	 * in the table -1 is returned.
	 * @param M_Product_ID
	 * @return  Row of the product or -1 if non existing.
	 * 
	 */
	private int findProductRow(int M_Product_ID)
	{
		//DefaultTableModel model = (DefaultTableModel)dialog.getMiniTable().getModel();
		ListModelTable model = (ListModelTable) window.getWListbox().getModel();
		KeyNamePair kp;
		for (int i=0; i<model.getRowCount(); i++) {
			kp = (KeyNamePair)model.getValueAt(i, 4);
			if (kp.getKey()==M_Product_ID) {
				return(i);
			}
		}
		return(-1);
	}
		
	/**
	 *  Change Listener
	 *  @param e event
	 */
	public void valueChange (ValueChangeEvent e)
	{
		if (log.isLoggable(Level.CONFIG)) log.config(e.getPropertyName() + "=" + e.getNewValue());

		//  BPartner - load Order/Invoice/Shipment
		if (e.getPropertyName().equals("C_BPartner_ID"))
		{
			int C_BPartner_ID = 0; 
			if (e.getNewValue() != null){
				C_BPartner_ID = ((Integer)e.getNewValue()).intValue();
			}
			
			initBPOrderDetails (C_BPartner_ID, false);
		}
		window.tableChanged(null);
	}   //  vetoableChange
	
	/**************************************************************************
	 *  Load BPartner Field
	 *  @param forInvoice true if Invoices are to be created, false receipts
	 *  @throws Exception if Lookups cannot be initialized
	 */
	protected void initBPartner (boolean forInvoice) throws Exception
	{
		//  load BPartner
		int AD_Column_ID = 3499;        //  C_Invoice.C_BPartner_ID
		MLookup lookup = MLookupFactory.get (Env.getCtx(), p_WindowNo, 0, AD_Column_ID, DisplayType.Search);
		bPartnerField = new WSearchEditor ("C_BPartner_ID", true, false, true, lookup);
		//
		int C_BPartner_ID = Env.getContextAsInt(Env.getCtx(), p_WindowNo, "C_BPartner_ID");
		bPartnerField.setValue(Integer.valueOf(C_BPartner_ID));

		//  initial loading
		initBPOrderDetails(C_BPartner_ID, forInvoice);
	}   //  initBPartner

	/**
	 * Init Details - load invoices not shipped
	 * @param C_BPartner_ID BPartner
	 */
	private void initBPInvoiceDetails(int C_BPartner_ID)
	{
		if (log.isLoggable(Level.CONFIG)) log.config("C_BPartner_ID" + C_BPartner_ID);

		//  load Shipments (Receipts) - Completed, Closed
		invoiceField.removeActionListener(this);
		invoiceField.removeAllItems();
		//	None
		KeyNamePair pp = new KeyNamePair(0,"");
		invoiceField.addItem(pp);
		
		ArrayList<KeyNamePair> list = loadInvoiceData(C_BPartner_ID);
		for(KeyNamePair knp : list)
			invoiceField.addItem(knp);
		
		invoiceField.setSelectedIndex(0);
		invoiceField.addActionListener(this);
		upcField.addValueChangeListener(this);
	}
	
	/**
	 *  Load PBartner dependent Order/Invoice/Shipment Field.
	 *  @param C_BPartner_ID BPartner
	 *  @param forInvoice for invoice
	 */
	protected void initBPOrderDetails (int C_BPartner_ID, boolean forInvoice)
	{
		if (log.isLoggable(Level.CONFIG)) log.config("C_BPartner_ID=" + C_BPartner_ID);
		KeyNamePair pp = new KeyNamePair(0,"");
		//  load PO Orders - Closed, Completed
		orderField.removeActionListener(this);
		orderField.removeAllItems();
		orderField.addItem(pp);
		
		ArrayList<KeyNamePair> list = loadOrderData(C_BPartner_ID, forInvoice, sameWarehouseCb.isSelected());
		for(KeyNamePair knp : list)
			orderField.addItem(knp);
		
		orderField.setSelectedIndex(0);
		orderField.addActionListener(this);

		initBPDetails(C_BPartner_ID);
	}   //  initBPOrderDetails
	
	public void initBPDetails(int C_BPartner_ID) 
	{
		initBPInvoiceDetails(C_BPartner_ID);
		initBPRMADetails(C_BPartner_ID);
	}

	
	/**
	 * Load RMA that are candidates for shipment
	 * @param C_BPartner_ID BPartner
	 */
	private void initBPRMADetails(int C_BPartner_ID)
	{
	    rmaField.removeActionListener(this);
	    rmaField.removeAllItems();
	    //  None
	    KeyNamePair pp = new KeyNamePair(0,"");
	    rmaField.addItem(pp);
	    
	    ArrayList<KeyNamePair> list = loadRMAData(C_BPartner_ID);
		for(KeyNamePair knp : list)
			rmaField.addItem(knp);
		
	    rmaField.setSelectedIndex(0);
	    rmaField.addActionListener(this);
	}

	/**
	 *  Load Data - Order
	 *  @param C_Order_ID Order
	 *  @param forInvoice true if for invoice vs. delivery qty
	 */
/*	protected void loadOrder (int C_Order_ID, boolean forInvoice)
	{
		loadTableOIS(getOrderData(C_Order_ID, forInvoice));
	}   //  LoadOrder
	
	protected void loadRMA (int M_RMA_ID)
	{
		loadTableOIS(getRMAData(M_RMA_ID));
	}
	
	protected void loadShipment (int M_InOut_ID)
	{
		loadTableOIS(getShipmentData(M_InOut_ID));
	}*/
	
	/**
	 *  Load Data - Order
	 *  @param C_Order_ID Order
	 *  @param forInvoice true if for invoice vs. delivery qty
	 *  @param M_Locator_ID
	 */
	protected void loadOrder (int C_Order_ID, boolean forInvoice, int M_Locator_ID)
	{
		loadTableOIS(getOrderData(C_Order_ID, forInvoice, M_Locator_ID));
	}   //  LoadOrder
	
	/**
	 *  Load Data - RMA
	 *  @param M_RMA_ID RMA
	 *  @param M_Locator_ID
	 */
	protected void loadRMA (int M_RMA_ID, int M_Locator_ID)
	{
		loadTableOIS(getRMAData(M_RMA_ID, M_Locator_ID));
	}
		
	/**
	 *  Load Data - Invoice
	 *  @param C_Invoice_ID Invoice
	 *  @param M_Locator_ID
	 */
	protected void loadInvoice (int C_Invoice_ID, int M_Locator_ID)
	{
		loadTableOIS(getInvoiceData(C_Invoice_ID, M_Locator_ID));
	}
		
	/**
	 *  Load Order/Invoice/Shipment data into Table
	 *  @param data data
	 */
	protected void loadTableOIS (Vector<?> data)
	{
		window.getWListbox().clear();
		
		//  Remove previous listeners
		window.getWListbox().getModel().removeTableModelListener(window);
		//  Set Model
		ListModelTable model = new ListModelTable(data);
		model.addTableModelListener(window);
		window.getWListbox().setData(model, getOISColumnNames());
		//
		
		configureMiniTable(window.getWListbox());
	}   //  loadOrder
	
	public void showWindow()
	{
		window.setVisible(true);
	}
	
	public void closeWindow()
	{
		window.dispose();
	}

	@Override
	public Object getWindow() {
		return window;
	}
	
	protected void setupColumns(Grid parameterGrid) {
		noOfParameterColumn = ClientInfo.maxWidth((ClientInfo.EXTRA_SMALL_WIDTH+ClientInfo.SMALL_WIDTH)/2) ? 2 : 4;
		Columns columns = new Columns();
		parameterGrid.appendChild(columns);
		if (ClientInfo.maxWidth((ClientInfo.EXTRA_SMALL_WIDTH+ClientInfo.SMALL_WIDTH)/2))
		{
			Column column = new Column();
			ZKUpdateUtil.setWidth(column, "35%");
			columns.appendChild(column);
			column = new Column();
			ZKUpdateUtil.setWidth(column, "65%");
			columns.appendChild(column);
		}
		else
		{
			Column column = new Column();
			columns.appendChild(column);		
			column = new Column();
			ZKUpdateUtil.setWidth(column, "15%");
			columns.appendChild(column);
			ZKUpdateUtil.setWidth(column, "35%");
			column = new Column();
			ZKUpdateUtil.setWidth(column, "15%");
			columns.appendChild(column);
			column = new Column();
			ZKUpdateUtil.setWidth(column, "35%");
			columns.appendChild(column);
		}
	}
	
	protected void onClientInfo()
	{
		if (ClientInfo.isMobile() && parameterStdLayout != null && parameterStdLayout.getRows() != null)
		{
			int nc = ClientInfo.maxWidth((ClientInfo.EXTRA_SMALL_WIDTH+ClientInfo.SMALL_WIDTH)/2) ? 2 : 4;
			int cc = noOfParameterColumn;
			if (nc == cc)
				return;
			
			parameterStdLayout.getColumns().detach();
			setupColumns(parameterStdLayout);
			if (cc > nc)
			{
				LayoutUtils.compactTo(parameterStdLayout, nc);
			}
			else
			{
				LayoutUtils.expandTo(parameterStdLayout, nc, false);
			}
			
			ZKUpdateUtil.setCSSHeight(window);
			ZKUpdateUtil.setCSSWidth(window);
			window.invalidate();			
		}
	}
	/**
	 *  Load PBartner dependent Order/Invoice/Shipment Field.
	 *  @param C_BPartner_ID BPartner
	 */
	protected ArrayList<KeyNamePair> loadRMAData(int C_BPartner_ID) {
		ArrayList<KeyNamePair> list = new ArrayList<KeyNamePair>();

		String sqlStmt = "SELECT r.M_RMA_ID, r.DocumentNo || '-' || r.Amt from M_RMA r "
			+ "WHERE ISSOTRX='Y' AND r.DocStatus in ('CO', 'CL') " 
			+ "AND r.C_BPartner_ID=? "
			+ "AND r.M_RMA_ID in (SELECT rl.M_RMA_ID FROM M_RMALine rl "
			+ "WHERE rl.M_RMA_ID=r.M_RMA_ID AND rl.QtyDelivered < rl.Qty " 
			+ "AND rl.M_InOutLine_ID IS NOT NULL)";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sqlStmt, null);
			pstmt.setInt(1, C_BPartner_ID);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(new KeyNamePair(rs.getInt(1), rs.getString(2)));
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, sqlStmt.toString(), e);
		} finally{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return list;
	}

	/**
	 * Load PBartner dependent Order/Invoice/Shipment Field.
	 * @param C_BPartner_ID
	 */
	protected ArrayList<KeyNamePair> loadInvoiceData (int C_BPartner_ID)
	{
		ArrayList<KeyNamePair> list = new ArrayList<KeyNamePair>();
		
		StringBuffer display = new StringBuffer("i.DocumentNo||' - '||")
		.append(DB.TO_CHAR("DateInvoiced", DisplayType.Date, Env.getAD_Language(Env.getCtx())))
		.append("|| ' - ' ||")
		.append(DB.TO_CHAR("GrandTotal", DisplayType.Amount, Env.getAD_Language(Env.getCtx())));
		//
		StringBuffer sql = new StringBuffer("SELECT i.C_Invoice_ID,").append(display)
		.append(" FROM C_Invoice i "
				+ "WHERE i.C_BPartner_ID=? AND i.IsSOTrx='N' AND i.DocStatus IN ('CL','CO')"
				+ " AND i.C_Invoice_ID IN "
				+ "(SELECT il.C_Invoice_ID FROM C_InvoiceLine il"
				+ " LEFT OUTER JOIN M_MatchInv mi ON (il.C_InvoiceLine_ID=mi.C_InvoiceLine_ID) "
				+ " JOIN C_Invoice i2 ON (il.C_Invoice_ID = i2.C_Invoice_ID) "
				+ " WHERE i2.C_BPartner_ID=? AND i2.IsSOTrx='N' AND i2.DocStatus IN ('CL','CO') "
				+ "GROUP BY il.C_Invoice_ID,mi.C_InvoiceLine_ID,il.QtyInvoiced "
				+ "HAVING (il.QtyInvoiced<>SUM(mi.Qty) AND mi.C_InvoiceLine_ID IS NOT NULL)"
				+ " OR mi.C_InvoiceLine_ID IS NULL) "
				+ "ORDER BY i.DateInvoiced");

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, C_BPartner_ID);
			pstmt.setInt(2, C_BPartner_ID);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				list.add(new KeyNamePair(rs.getInt(1), rs.getString(2)));
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return list;
	}
	
	/**
	 *  Load Data - Order
	 *  @param C_Order_ID Order
	 *  @param forInvoice true if for invoice vs. delivery qty
	 */
	protected Vector<Vector<Object>> getOrderData (int C_Order_ID, boolean forInvoice)
	{
		/**
		 *  Selected        - 0
		 *  Qty             - 1
		 *  C_UOM_ID        - 2
		 *  M_Locator_ID    - 3
		 *  M_Product_ID    - 4
		 *  VendorProductNo - 5
		 *  OrderLine       - 6
		 *  ShipmentLine    - 7
		 *  InvoiceLine     - 8
		 */
		if (log.isLoggable(Level.CONFIG)) log.config("C_Order_ID=" + C_Order_ID);
		p_order = new MOrder (Env.getCtx(), C_Order_ID, null);      //  save

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		StringBuilder sql = new StringBuilder("SELECT "
				+ "l.QtyOrdered-SUM(COALESCE(m.Qty,0))"
				// subtract drafted lines from this or other orders IDEMPIERE-2889
				+ "-COALESCE((SELECT SUM(MovementQty) FROM M_InOutLine iol JOIN M_InOut io ON iol.M_InOut_ID=io.M_InOut_ID WHERE l.C_OrderLine_ID=iol.C_OrderLine_ID AND io.Processed='N'),0),"	//	1
				+ "CASE WHEN l.QtyOrdered=0 THEN 0 ELSE l.QtyEntered/l.QtyOrdered END,"	//	2
				+ " l.C_UOM_ID,COALESCE(uom.UOMSymbol,uom.Name),"			//	3..4
				+ " p.M_Locator_ID, loc.Value, " // 5..6
				+ " COALESCE(l.M_Product_ID,0),COALESCE(p.Name,c.Name), " //	7..8
				+ " po.VendorProductNo, " // 9
				+ " l.C_OrderLine_ID,l.Line "	//	10..11
				+ "FROM C_OrderLine l"
				+ " LEFT OUTER JOIN M_Product_PO po ON (l.M_Product_ID = po.M_Product_ID AND l.C_BPartner_ID = po.C_BPartner_ID) "
				+ " LEFT OUTER JOIN M_MatchPO m ON (l.C_OrderLine_ID=m.C_OrderLine_ID AND ");
		sql.append(forInvoice ? "m.C_InvoiceLine_ID" : "m.M_InOutLine_ID");
		sql.append(" IS NOT NULL)")
		.append(" LEFT OUTER JOIN M_Product p ON (l.M_Product_ID=p.M_Product_ID)"
				+ " LEFT OUTER JOIN M_Locator loc on (p.M_Locator_ID=loc.M_Locator_ID)"
				+ " LEFT OUTER JOIN C_Charge c ON (l.C_Charge_ID=c.C_Charge_ID)");
		if (Env.isBaseLanguage(Env.getCtx(), "C_UOM"))
			sql.append(" LEFT OUTER JOIN C_UOM uom ON (l.C_UOM_ID=uom.C_UOM_ID)");
		else
			sql.append(" LEFT OUTER JOIN C_UOM_Trl uom ON (l.C_UOM_ID=uom.C_UOM_ID AND uom.AD_Language='")
			.append(Env.getAD_Language(Env.getCtx())).append("')");
		//
		sql.append(" WHERE l.C_Order_ID=? "			//	#1
				+ "GROUP BY l.QtyOrdered,CASE WHEN l.QtyOrdered=0 THEN 0 ELSE l.QtyEntered/l.QtyOrdered END, "
				+ "l.C_UOM_ID,COALESCE(uom.UOMSymbol,uom.Name), p.M_Locator_ID, loc.Value, po.VendorProductNo, "
				+ "l.M_Product_ID,COALESCE(p.Name,c.Name), l.Line,l.C_OrderLine_ID "
				+ "ORDER BY l.Line");
		//
		if (log.isLoggable(Level.FINER)) log.finer(sql.toString());
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, C_Order_ID);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				Vector<Object> line = new Vector<Object>();
				line.add(Boolean.FALSE);           //  0-Selection
				BigDecimal qtyOrdered = rs.getBigDecimal(1);
				BigDecimal multiplier = rs.getBigDecimal(2);
				BigDecimal qtyEntered = qtyOrdered.multiply(multiplier);
				line.add(qtyEntered);  //  1-Qty
				KeyNamePair pp = new KeyNamePair(rs.getInt(3), rs.getString(4).trim());
				line.add(pp);                           //  2-UOM
				// Add locator
				line.add(getLocatorKeyNamePair(rs.getInt(5)));// 3-Locator
				// Add product
				pp = new KeyNamePair(rs.getInt(7), rs.getString(8));
				line.add(pp);                           //  4-Product
				line.add(rs.getString(9));				// 5-VendorProductNo
				pp = new KeyNamePair(rs.getInt(10), rs.getString(11));
				line.add(pp);                           //  6-OrderLine
				line.add(null);                         //  7-Ship
				line.add(null);                         //  8-Invoice
				data.add(line);
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
			//throw new DBException(e, sql.toString());
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		return data;
	}   //  LoadOrder
	
	/**
	 * Load RMA details
	 * @param M_RMA_ID RMA
	 */
	protected Vector<Vector<Object>> getRMAData(int M_RMA_ID)
	{
		m_invoice = null;
		p_order = null;
		m_rma = new MRMA(Env.getCtx(), M_RMA_ID, null);
			
	    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
	    StringBuilder sqlStmt = new StringBuilder();
	    sqlStmt.append("SELECT rl.M_RMALine_ID, rl.line, rl.Qty - rl.QtyDelivered, p.M_Product_ID, COALESCE(p.Name, c.Name), uom.C_UOM_ID, COALESCE(uom.UOMSymbol,uom.Name) ");
	    sqlStmt.append("FROM M_RMALine rl INNER JOIN M_InOutLine iol ON rl.M_InOutLine_ID=iol.M_InOutLine_ID ");
	          
	    if (Env.isBaseLanguage(Env.getCtx(), "C_UOM"))
        {
	        sqlStmt.append("LEFT OUTER JOIN C_UOM uom ON (uom.C_UOM_ID=iol.C_UOM_ID) ");
        }
	    else
        {
	        sqlStmt.append("LEFT OUTER JOIN C_UOM_Trl uom ON (uom.C_UOM_ID=iol.C_UOM_ID AND uom.AD_Language='");
	        sqlStmt.append(Env.getAD_Language(Env.getCtx())).append("') ");
        }
	    sqlStmt.append("LEFT OUTER JOIN M_Product p ON p.M_Product_ID=iol.M_Product_ID ");
	    sqlStmt.append("LEFT OUTER JOIN C_Charge c ON c.C_Charge_ID=iol.C_Charge_ID ");
	    sqlStmt.append("WHERE rl.M_RMA_ID=? ");
	    sqlStmt.append("AND rl.M_InOutLine_ID IS NOT NULL");
	           
	    sqlStmt.append(" UNION ");
	    
	    sqlStmt.append("SELECT rl.M_RMALine_ID, rl.line, rl.Qty - rl.QtyDelivered, p.M_Product_ID, p.Name, uom.C_UOM_ID, COALESCE(uom.UOMSymbol,uom.Name) ");
	    sqlStmt.append("FROM M_RMALine rl INNER JOIN M_Product p ON p.M_Product_ID = rl.M_Product_ID ");
	    if (Env.isBaseLanguage(Env.getCtx(), "C_UOM"))
        {
	        sqlStmt.append("LEFT OUTER JOIN C_UOM uom ON (uom.C_UOM_ID=p.C_UOM_ID) ");
        }
	    else
        {
	        sqlStmt.append("LEFT OUTER JOIN C_UOM_Trl uom ON (uom.C_UOM_ID=100 AND uom.AD_Language='");
	        sqlStmt.append(Env.getAD_Language(Env.getCtx())).append("') ");
        }
	    sqlStmt.append("WHERE rl.M_RMA_ID=? ");
	    sqlStmt.append("AND rl.M_Product_ID IS NOT NULL AND rl.M_InOutLine_ID IS NULL");
	    
	    sqlStmt.append(" UNION ");
	            
	    sqlStmt.append("SELECT rl.M_RMALine_ID, rl.line, rl.Qty - rl.QtyDelivered, 0, c.Name, uom.C_UOM_ID, COALESCE(uom.UOMSymbol,uom.Name) ");
	    sqlStmt.append("FROM M_RMALine rl INNER JOIN C_Charge c ON c.C_Charge_ID = rl.C_Charge_ID ");
	    if (Env.isBaseLanguage(Env.getCtx(), "C_UOM"))
        {
	        sqlStmt.append("LEFT OUTER JOIN C_UOM uom ON (uom.C_UOM_ID=100) ");
        }
	    else
        {
	        sqlStmt.append("LEFT OUTER JOIN C_UOM_Trl uom ON (uom.C_UOM_ID=100 AND uom.AD_Language='");
	        sqlStmt.append(Env.getAD_Language(Env.getCtx())).append("') ");
        }
	    sqlStmt.append("WHERE rl.M_RMA_ID=? ");
	    sqlStmt.append("AND rl.C_Charge_ID IS NOT NULL AND rl.M_InOutLine_ID IS NULL");
	    sqlStmt.append(" ORDER BY 2");
     
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try
	    {
	        pstmt = DB.prepareStatement(sqlStmt.toString(), null);
	        pstmt.setInt(1, M_RMA_ID);
	        pstmt.setInt(2, M_RMA_ID);
	        pstmt.setInt(3, M_RMA_ID);
	        rs = pstmt.executeQuery();
	               
	        while (rs.next())
            {
	            Vector<Object> line = new Vector<Object>(7);
	            line.add(Boolean.FALSE);   // 0-Selection
	            line.add(rs.getBigDecimal(3));  // 1-Qty
	            KeyNamePair pp = new KeyNamePair(rs.getInt(6), rs.getString(7));
	            line.add(pp); // 2-UOM
				line.add(getLocatorKeyNamePair(0));
	            pp = new KeyNamePair(rs.getInt(4), rs.getString(5));
				line.add(pp); // 4-Product
				line.add(null); //5-Vendor Product No
				line.add(null); //6-Order
				pp = new KeyNamePair(rs.getInt(1), rs.getString(2));
				line.add(pp);   //7-RMA
				line.add(null); //8-invoice
	            data.add(line);
            }
	    }
	    catch (Exception ex)
	    {
	        log.log(Level.SEVERE, sqlStmt.toString(), ex);
	    }
	    finally
	    {
	    	DB.close(rs, pstmt);
	    	rs = null; pstmt = null;
	    }
	    
	    return data;
	}

	/**
	 * Load Invoice details
	 * @param C_Invoice_ID Invoice
	 */
	protected Vector<Vector<Object>> getInvoiceData(int C_Invoice_ID)
	{
		m_invoice = new MInvoice(Env.getCtx(), C_Invoice_ID, null); // save
		p_order = null;
		m_rma = null;
		
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		StringBuilder sql = new StringBuilder("SELECT " // Entered UOM
				+ "l.QtyInvoiced-SUM(NVL(mi.Qty,0)),l.QtyEntered/l.QtyInvoiced,"
				+ " l.C_UOM_ID,COALESCE(uom.UOMSymbol,uom.Name)," // 3..4
				+ " p.M_Locator_ID, loc.Value, " // 5..6
				+ " l.M_Product_ID,p.Name, po.VendorProductNo, l.C_InvoiceLine_ID,l.Line," // 7..11
				+ " l.C_OrderLine_ID " // 12
				+ " FROM C_InvoiceLine l "); 
		if (Env.isBaseLanguage(Env.getCtx(), "C_UOM"))
			sql.append(" LEFT OUTER JOIN C_UOM uom ON (l.C_UOM_ID=uom.C_UOM_ID)");
		else
			sql.append(" LEFT OUTER JOIN C_UOM_Trl uom ON (l.C_UOM_ID=uom.C_UOM_ID AND uom.AD_Language='")
			.append(Env.getAD_Language(Env.getCtx())).append("')");

		sql.append(" LEFT OUTER JOIN M_Product p ON (l.M_Product_ID=p.M_Product_ID)")
		.append(" LEFT OUTER JOIN M_Locator loc on (p.M_Locator_ID=loc.M_Locator_ID)")
		.append(" INNER JOIN C_Invoice inv ON (l.C_Invoice_ID=inv.C_Invoice_ID)")
		.append(" LEFT OUTER JOIN M_Product_PO po ON (l.M_Product_ID = po.M_Product_ID AND inv.C_BPartner_ID = po.C_BPartner_ID)")
		.append(" LEFT OUTER JOIN M_MatchInv mi ON (l.C_InvoiceLine_ID=mi.C_InvoiceLine_ID)")

		.append(" WHERE l.C_Invoice_ID=? AND l.QtyInvoiced<>0 ")
		.append("GROUP BY l.QtyInvoiced,l.QtyEntered/l.QtyInvoiced,"
				+ "l.C_UOM_ID,COALESCE(uom.UOMSymbol,uom.Name),"
				+ "p.M_Locator_ID, loc.Value, "
				+ "l.M_Product_ID,p.Name, po.VendorProductNo, l.C_InvoiceLine_ID,l.Line,l.C_OrderLine_ID ")
				.append("ORDER BY l.Line");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, C_Invoice_ID);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				Vector<Object> line = new Vector<Object>(7);
				line.add(Boolean.FALSE); // 0-Selection
				BigDecimal qtyInvoiced = rs.getBigDecimal(1);
				BigDecimal multiplier = rs.getBigDecimal(2);
				BigDecimal qtyEntered = qtyInvoiced.multiply(multiplier);
				line.add(qtyEntered); // 1-Qty
				KeyNamePair pp = new KeyNamePair(rs.getInt(3), rs.getString(4).trim());
				line.add(pp); // 2-UOM
				// Add locator
				line.add(getLocatorKeyNamePair(rs.getInt(5))); // 3-Locator
				pp = new KeyNamePair(rs.getInt(7), rs.getString(8));
				line.add(pp); // 4-Product
				line.add(rs.getString(9));				// 5-VendorProductNo
				int C_OrderLine_ID = rs.getInt(12);
				if (rs.wasNull())
					line.add(null); // 6-Order
				else
					line.add(new KeyNamePair(C_OrderLine_ID, "."));
				line.add(null); // 7-Ship
				pp = new KeyNamePair(rs.getInt(10), rs.getString(11));
				line.add(pp); // 8-Invoice
				data.add(line);
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
			//throw new DBException(e, sql);
		}
	    finally
	    {
	    	DB.close(rs, pstmt);
	    	rs = null; pstmt = null;
	    }
		return data;
	}
	
	/**
	 * Get KeyNamePair for Locator.
	 * If no locator specified or the specified locator is not valid (e.g. warehouse not match),
	 * a default one will be used.
	 * @param M_Locator_ID
	 * @return KeyNamePair
	 */
	protected KeyNamePair getLocatorKeyNamePair(int M_Locator_ID)
	{
		MLocator locator = null;
		
		// Load desired Locator
		if (M_Locator_ID > 0)
		{
			locator = MLocator.get(Env.getCtx(), M_Locator_ID);
			// Validate warehouse
			if (locator != null && locator.getM_Warehouse_ID() != getM_Warehouse_ID())
			{
				locator = null;
			}
		}
		
		// Try to use default locator from Order Warehouse
		if (locator == null && p_order != null && p_order.getM_Warehouse_ID() == getM_Warehouse_ID())
		{
			MWarehouse wh = MWarehouse.get(Env.getCtx(), p_order.getM_Warehouse_ID());
			if (wh != null)
			{
				locator = wh.getDefaultLocator();
			}
		}
		// Try to get from locator field
		if (locator == null)
		{
			if (defaultLocator_ID > 0)
			{
				locator = MLocator.get(Env.getCtx(), defaultLocator_ID);
			}
		}
		// Validate Warehouse
		if (locator == null || locator.getM_Warehouse_ID() != getM_Warehouse_ID())
		{
			locator = MWarehouse.get(Env.getCtx(), getM_Warehouse_ID()).getDefaultLocator();
		}
		
		KeyNamePair pp = null ;
		if (locator != null)
		{
			pp = new KeyNamePair(locator.get_ID(), locator.getValue());
		}
		return pp;
	}
	
	/**
	 *  List number of rows selected
	 */
	public void info(IMiniTable miniTable, IStatusBar statusBar)
	{

	}   //  infoInvoice

	protected void configureMiniTable (IMiniTable miniTable)
	{
		miniTable.setColumnClass(0, Boolean.class, false);     //  Selection
		miniTable.setColumnClass(1, BigDecimal.class, false);      //  Qty
		miniTable.setColumnClass(2, String.class, true);          //  UOM
		miniTable.setColumnClass(3, String.class, false);  //  Locator
		miniTable.setColumnClass(4, String.class, true);   //  Product
		miniTable.setColumnClass(5, String.class, true); //  VendorProductNo
		miniTable.setColumnClass(6, String.class, true);     //  Order
		miniTable.setColumnClass(7, String.class, true);     //  Ship
		miniTable.setColumnClass(8, String.class, true);   //  Invoice
		
		//  Table UI
		miniTable.autoSize();
		
	}

	/**
	 *  Save - Create Invoice Lines
	 *  @return true if saved
	 */
	public boolean save(IMiniTable miniTable, String trxName)
	{
		/*
		dataTable.stopEditor(true);
		log.config("");
		TableModel model = dataTable.getModel();
		int rows = model.getRowCount();
		if (rows == 0)
			return false;
		//
		Integer defaultLoc = (Integer) locatorField.getValue();
		if (defaultLoc == null || defaultLoc.intValue() == 0) {
			locatorField.setBackground(AdempierePLAF.getFieldBackground_Error());
			return false;
		}
		*/
		int M_Locator_ID = defaultLocator_ID;
		if (M_Locator_ID == 0) {
			return false;
		}
		// Get Shipment
		int M_InOut_ID = ((Integer) getGridTab().getValue("M_InOut_ID")).intValue();
		MInOut inout = new MInOut(Env.getCtx(), M_InOut_ID, trxName);
		if (log.isLoggable(Level.CONFIG)) log.config(inout + ", C_Locator_ID=" + M_Locator_ID);

		// Lines
		for (int i = 0; i < miniTable.getRowCount(); i++)
		{
			if (((Boolean)miniTable.getValueAt(i, 0)).booleanValue()) {
				// variable values
				BigDecimal QtyEntered = (BigDecimal) miniTable.getValueAt(i, 1); // Qty
				KeyNamePair pp = (KeyNamePair) miniTable.getValueAt(i, 2); // UOM
				int C_UOM_ID = pp.getKey();
				pp = (KeyNamePair) miniTable.getValueAt(i, 3); // Locator
				// If a locator is specified on the product, choose that otherwise default locator
				M_Locator_ID = pp!=null && pp.getKey()!=0 ? pp.getKey() : defaultLocator_ID;

				pp = (KeyNamePair) miniTable.getValueAt(i, 4); // Product
				int M_Product_ID = pp.getKey();
				int C_OrderLine_ID = 0;
				pp = (KeyNamePair) miniTable.getValueAt(i, 6); // OrderLine
				if (pp != null)
					C_OrderLine_ID = pp.getKey();
				int M_RMALine_ID = 0;
				pp = (KeyNamePair) miniTable.getValueAt(i, 7); // RMA
				// If we have RMA
				if (pp != null)
					M_RMALine_ID = pp.getKey();
				int C_InvoiceLine_ID = 0;
				MInvoiceLine il = null;
				pp = (KeyNamePair) miniTable.getValueAt(i, 8); // InvoiceLine
				if (pp != null)
					C_InvoiceLine_ID = pp.getKey();
				if (C_InvoiceLine_ID != 0)
					il = new MInvoiceLine (Env.getCtx(), C_InvoiceLine_ID, trxName);
				//boolean isInvoiced = (C_InvoiceLine_ID != 0);
				//	Precision of Qty UOM
				int precision = 2;
				if (M_Product_ID != 0)
				{
					MProduct product = MProduct.get(Env.getCtx(), M_Product_ID);
					precision = product.getUOMPrecision();
				}
				QtyEntered = QtyEntered.setScale(precision, RoundingMode.HALF_DOWN);
				//
				if (log.isLoggable(Level.FINE)) log.fine("Line QtyEntered=" + QtyEntered
						+ ", Product=" + M_Product_ID 
						+ ", OrderLine=" + C_OrderLine_ID + ", InvoiceLine=" + C_InvoiceLine_ID);

				//	Credit Memo - negative Qty
				if (m_invoice != null && ((MInvoice) m_invoice).isCreditMemo() )
					QtyEntered = QtyEntered.negate();

				//	Create new InOut Line
				MInOutLine iol = new MInOutLine (inout);
				iol.setM_Product_ID(M_Product_ID, C_UOM_ID);	//	Line UOM
				iol.setQty(QtyEntered);							//	Movement/Entered
				//
				MOrderLine ol = null;
				MRMALine rmal = null;
				if (C_OrderLine_ID != 0)
				{
					iol.setC_OrderLine_ID(C_OrderLine_ID);
					ol = new MOrderLine (Env.getCtx(), C_OrderLine_ID, trxName);
					if (ol.getQtyEntered().compareTo(ol.getQtyOrdered()) != 0)
					{
						iol.setMovementQty(QtyEntered
								.multiply(ol.getQtyOrdered())
								.divide(ol.getQtyEntered(), 12, RoundingMode.HALF_UP));
						iol.setC_UOM_ID(ol.getC_UOM_ID());
					}
					iol.setM_AttributeSetInstance_ID(ol.getM_AttributeSetInstance_ID());
					iol.setDescription(ol.getDescription());
					//
					iol.setC_Project_ID(ol.getC_Project_ID());
					iol.setC_ProjectPhase_ID(ol.getC_ProjectPhase_ID());
					iol.setC_ProjectTask_ID(ol.getC_ProjectTask_ID());
					iol.setC_Activity_ID(ol.getC_Activity_ID());
					iol.setC_Campaign_ID(ol.getC_Campaign_ID());
					iol.setAD_OrgTrx_ID(ol.getAD_OrgTrx_ID());
					iol.setUser1_ID(ol.getUser1_ID());
					iol.setUser2_ID(ol.getUser2_ID());
				}
				else if (il != null)
				{
					if (il.getQtyEntered().compareTo(il.getQtyInvoiced()) != 0)
					{
						iol.setQtyEntered(QtyEntered
								.multiply(il.getQtyInvoiced())
								.divide(il.getQtyEntered(), 12, RoundingMode.HALF_UP));
						iol.setC_UOM_ID(il.getC_UOM_ID());
					}
					iol.setDescription(il.getDescription());
					iol.setC_Project_ID(il.getC_Project_ID());
					iol.setC_ProjectPhase_ID(il.getC_ProjectPhase_ID());
					iol.setC_ProjectTask_ID(il.getC_ProjectTask_ID());
					iol.setC_Activity_ID(il.getC_Activity_ID());
					iol.setC_Campaign_ID(il.getC_Campaign_ID());
					iol.setAD_OrgTrx_ID(il.getAD_OrgTrx_ID());
					iol.setUser1_ID(il.getUser1_ID());
					iol.setUser2_ID(il.getUser2_ID());
				}
				else if (M_RMALine_ID != 0)
				{
					rmal = new MRMALine(Env.getCtx(), M_RMALine_ID, trxName);
					iol.setM_RMALine_ID(M_RMALine_ID);
					iol.setQtyEntered(QtyEntered);
					iol.setDescription(rmal.getDescription());
					iol.setM_AttributeSetInstance_ID(rmal.getM_AttributeSetInstance_ID());
					iol.setC_Project_ID(rmal.getC_Project_ID());
					iol.setC_ProjectPhase_ID(rmal.getC_ProjectPhase_ID());
					iol.setC_ProjectTask_ID(rmal.getC_ProjectTask_ID());
					iol.setC_Activity_ID(rmal.getC_Activity_ID());
					iol.setAD_OrgTrx_ID(rmal.getAD_OrgTrx_ID());
					iol.setUser1_ID(rmal.getUser1_ID());
					iol.setUser2_ID(rmal.getUser2_ID());
				}

				//	Charge
				if (M_Product_ID == 0)
				{
					if (ol != null && ol.getC_Charge_ID() != 0)			//	from order
						iol.setC_Charge_ID(ol.getC_Charge_ID());
					else if (il != null && il.getC_Charge_ID() != 0)	//	from invoice
						iol.setC_Charge_ID(il.getC_Charge_ID());
					else if (rmal != null && rmal.getC_Charge_ID() != 0) // from rma
						iol.setC_Charge_ID(rmal.getC_Charge_ID());
				}
				// Set locator
				iol.setM_Locator_ID(M_Locator_ID);
				iol.saveEx();
				//	Create Invoice Line Link
				if (il != null)
				{
					il.setM_InOutLine_ID(iol.getM_InOutLine_ID());
					il.saveEx();
				}
			}   //   if selected
		}   //  for all rows

		/**
		 *  Update Header
		 *  - if linked to another order/invoice/rma - remove link
		 *  - if no link set it
		 */
		if (p_order != null && p_order.getC_Order_ID() != 0)
		{
			inout.setC_Order_ID (p_order.getC_Order_ID());
			inout.setAD_OrgTrx_ID(p_order.getAD_OrgTrx_ID());
			inout.setC_Project_ID(p_order.getC_Project_ID());
			inout.setC_Campaign_ID(p_order.getC_Campaign_ID());
			inout.setC_Activity_ID(p_order.getC_Activity_ID());
			inout.setUser1_ID(p_order.getUser1_ID());
			inout.setUser2_ID(p_order.getUser2_ID());

			if ( p_order.isDropShip() )
			{
				inout.setM_Warehouse_ID( p_order.getM_Warehouse_ID() );
				inout.setIsDropShip(p_order.isDropShip());
				inout.setDropShip_BPartner_ID(p_order.getDropShip_BPartner_ID());
				inout.setDropShip_Location_ID(p_order.getDropShip_Location_ID());
				inout.setDropShip_User_ID(p_order.getDropShip_User_ID());
			}
		}
		if (m_invoice != null && ((X_M_InOut) m_invoice).getC_Invoice_ID() != 0)
		{
			if (inout.getC_Order_ID() == 0)
				inout.setC_Order_ID (((X_M_InOut) m_invoice).getC_Order_ID());
			inout.setC_Invoice_ID (((X_M_InOut) m_invoice).getC_Invoice_ID());
			inout.setAD_OrgTrx_ID(((X_M_InOut) m_invoice).getAD_OrgTrx_ID());
			inout.setC_Project_ID(((X_M_InOut) m_invoice).getC_Project_ID());
			inout.setC_Campaign_ID(((X_M_InOut) m_invoice).getC_Campaign_ID());
			inout.setC_Activity_ID(((X_M_InOut) m_invoice).getC_Activity_ID());
			inout.setUser1_ID(((X_M_InOut) m_invoice).getUser1_ID());
			inout.setUser2_ID(((X_M_InOut) m_invoice).getUser2_ID());
		}
		if (m_rma != null && m_rma.getM_RMA_ID() != 0)
		{
			MInOut originalIO = m_rma.getShipment();
			inout.setIsSOTrx(m_rma.isSOTrx());
			inout.setC_Order_ID(0);
			inout.setC_Invoice_ID(0);
			inout.setM_RMA_ID(m_rma.getM_RMA_ID());
			inout.setAD_OrgTrx_ID(originalIO.getAD_OrgTrx_ID());
			inout.setC_Project_ID(originalIO.getC_Project_ID());
			inout.setC_Campaign_ID(originalIO.getC_Campaign_ID());
			inout.setC_Activity_ID(originalIO.getC_Activity_ID());
			inout.setUser1_ID(originalIO.getUser1_ID());
			inout.setUser2_ID(originalIO.getUser2_ID());
		}
		inout.saveEx();
		return true;		

	}   //  saveInvoice

	protected Vector<String> getOISColumnNames()
	{
		//  Header Info
	    Vector<String> columnNames = new Vector<String>(7);
	    columnNames.add(Msg.getMsg(Env.getCtx(), "Select"));
	    columnNames.add(Msg.translate(Env.getCtx(), "Quantity"));
	    columnNames.add(Msg.translate(Env.getCtx(), "C_UOM_ID"));
	    columnNames.add(Msg.translate(Env.getCtx(), "M_Locator_ID"));
	    columnNames.add(Msg.translate(Env.getCtx(), "M_Product_ID"));
	    columnNames.add(Msg.getElement(Env.getCtx(), "VendorProductNo", false));
	    columnNames.add(Msg.getElement(Env.getCtx(), "C_Order_ID", false));
	    columnNames.add(Msg.getElement(Env.getCtx(), "M_RMA_ID", false));
	    columnNames.add(Msg.getElement(Env.getCtx(), "C_Invoice_ID", false));
	    
	    return columnNames;
	}

	protected Vector<Vector<Object>> getOrderData (int C_Order_ID, boolean forInvoice, int M_Locator_ID)
	{
		defaultLocator_ID = M_Locator_ID;
		return getOrderData (C_Order_ID, forInvoice);
	}

	protected Vector<Vector<Object>> getRMAData (int M_RMA_ID, int M_Locator_ID)
	{
		defaultLocator_ID = M_Locator_ID;
		return getRMAData (M_RMA_ID);
	}

	protected Vector<Vector<Object>> getInvoiceData (int C_Invoice_ID, int M_Locator_ID)
	{
		defaultLocator_ID = M_Locator_ID;
		return getInvoiceData (C_Invoice_ID);
	}
}