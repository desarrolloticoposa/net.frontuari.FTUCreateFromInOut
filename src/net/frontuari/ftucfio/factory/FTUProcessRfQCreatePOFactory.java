package net.frontuari.ftucfio.factory;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

import net.frontuari.ftucfio.process.FTURfQCreatePO;

public class FTUProcessRfQCreatePOFactory implements IProcessFactory{

	@Override
	public ProcessCall newProcessInstance(String className) {
		
		if(className.equals("net.frontuari.ftucfio.process.FTURfQCreatePO"))
			return new FTURfQCreatePO();
		return null;
	}

}
