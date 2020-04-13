package net.frontuari.ftucfio.factory;

import org.compiere.grid.ICreateFrom;
import org.compiere.grid.ICreateFromFactory;
import org.compiere.model.GridTab;
import org.compiere.model.I_M_InOut;

import net.frontuari.ftucfio.webui.apps.form.WFTUCreateFromShipmentUI;
/**
 * 
 * @author Carlos Vargas cvargas@frontuari.net
 *
 */
public class FTUCreateFromInOutFactory implements ICreateFromFactory {
	@Override
	public ICreateFrom create(GridTab mTab) 
	{
		String tableName = mTab.getTableName();
		if (tableName.equals(I_M_InOut.Table_Name))
			return new WFTUCreateFromShipmentUI(mTab);
		
		return null;
	}
}

