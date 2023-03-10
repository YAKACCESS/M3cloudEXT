/*
 ***************************************************************
 *                                                             *
 *                           NOTICE                            *
 *                                                             *
 *   THIS SOFTWARE IS THE PROPERTY OF AND CONTAINS             *
 *   CONFIDENTIAL INFORMATION OF INFOR AND/OR ITS AFFILIATES   *
 *   OR SUBSIDIARIES AND SHALL NOT BE DISCLOSED WITHOUT PRIOR  *
 *   WRITTEN PERMISSION. LICENSED CUSTOMERS MAY COPY AND       *
 *   ADAPT THIS SOFTWARE FOR THEIR OWN USE IN ACCORDANCE WITH  *
 *   THE TERMS OF THEIR SOFTWARE LICENSE AGREEMENT.            *
 *   ALL OTHER RIGHTS RESERVED.                                *
 *                                                             *
 *   (c) COPYRIGHT 2013 INFOR.  ALL RIGHTS RESERVED.           *
 *   THE WORD AND DESIGN MARKS SET FORTH HEREIN ARE            *
 *   TRADEMARKS AND/OR REGISTERED TRADEMARKS OF INFOR          *
 *   AND/OR ITS AFFILIATES AND SUBSIDIARIES. ALL RIGHTS        *
 *   RESERVED.  ALL OTHER TRADEMARKS LISTED HEREIN ARE         *
 *   THE PROPERTY OF THEIR RESPECTIVE OWNERS.                  *
 *                                                             *
 ***************************************************************
 */
package mvx.app.pgm.customer;

import mvx.app.common.*;
import mvx.runtime.*;
import mvx.db.dta.*;
import mvx.app.util.*;
import mvx.app.plist.*;
import mvx.app.ds.*;
import mvx.util.*;

/*
 *Modification area - M3
 *Nbr            Date   User id     Description
 *99999999999999 999999 XXXXXXXXXX  x
 *Modification area - Business partner
 *Nbr            Date   User id     Description
 *99999999999999 999999 XXXXXXXXXX  x
 *Modification area - Customer
 *Nbr            Date   User id     Description
 *        JNS012 170317 SOHCHA      DO Reversal
 *      JNS01200 170321 WILDSO      Rename DO Reversal to Reverse Pick
 *      JNS01201 170321 WILDSO      Mandatory Checks not handled
 *      JNS01202 170327 SOHCHA      Pick reversal for multiple lines
 *      JNS01203 170329 JYOSHI      Create revese Pick reciept
 *      JNS01204 170407 JYOSHI      Correction to reverse picklist
 *     XPRD00004 170802 WILDSO      Programs PRD movement
 *   XPRD0000508 170920 JYOSHI      CRCU20MI not throwing error messages
 *   XPRD0000509 170925 JYOSHI      CRCU20MI is not error messages
 *   XPRD0000511 171003 JYOSHI      Do not delete if the order status is 99 and if DO line exists
 *     XPRD00742 210113 SANDESH.PAWAR Changes in CRCU20MI to alow non stock item to reverse picking 
 */

/**
 *<BR><B><FONT SIZE=+2>Api: Facility</FONT></B><BR><BR>
 *
 * This class ...<BR><BR>
 *
 */
public class CRCU20MI extends MIBatch
{
	public void movexMain() {
		// ****************************************************************
		INIT();
		//   Accept conversation
		MICommon.accept();
		while (MICommon.read()) {
			//   Execute command
			if (MICommon.isTransaction(GET_USER_INFO)) {
				MICommon.setTransaction(retrieveUserInfo.getMessage());
				//} else if (MICommon.isTransaction("CrtReversePick")) { 		//D	JNS01200 170321
			} else if (MICommon.isTransaction("CrtReversePick")) { 		//A	JNS01200 170321
				RDOREV();
			}else if (MICommon.isTransaction("CrtReverseRec")) { 		 		//A	JNS01203 170328
				RDOREC(); 		//A	JNS01203 170328
			}else if (MICommon.isTransaction("DltDO")) { 		 		//A	JNS01203 170328
				RDODEL(); 		//A	JNS01203 170328
			}
			else if (MICommon.isTransaction("CrtReversePKNS")) { 		 		 		//A	XPRD00742 201222
				PICKREVNS(); 		 		//A	XPRD00742 201222
			}else {
				MICommon.setTransactionError();
			}
			MICommon.write();
		}
		//   Deallocate
		MICommon.close();
		SETLR();
		return;
	}

	/**
	 *    RDOREV  - Execute command Create DO Reversal
	 */
	public void RDOREV() {      
		sCRCU20MIRCrtReversePick inRevPick = (sCRCU20MIRCrtReversePick)MICommon.getInDS(sCRCU20MIRCrtReversePick.class);
		inRevPick.set().moveLeftPad(MICommon.getData());
		//   Convert company to numeric
		if (MICommon.toNumericCompany(inRevPick.getQ0CONO())) {
			XXCONO = MICommon.getInt();
		} else {
			MICommon.setError("CONO");
			return;
		}
		int plsx = 0; 		//A	JNS01201 170321
		long dlix = 0; 		//A	JNS01201 170321
		if(!inRevPick.getQ0PLSX().toString().trim().isEmpty()) 		//A	JNS01201 170321
			plsx = Integer.parseInt(inRevPick.getQ0PLSX().toString().trim()); 		//A	JNS01201 170321
		if(!inRevPick.getQ0DLIX().toString().trim().isEmpty()) 		//A	JNS01201 170321
			dlix = Long.parseLong(inRevPick.getQ0DLIX().toString().trim()); 		//A	JNS01201 170321
		//   Check Delivery number / Order index if blank
		//   MSGID=WDL0202 Delivery number must be entered
		if (inRevPick.getQ0DLIX().isBlank()){ 		//A	XPRD0000508 170920
			MICommon.setError("DLIX", "WDL0202"); 		//A	XPRD0000508 170920
			return; 		//A	XPRD0000508 170920
		} 		//A	XPRD0000508 170920

		//read MHDISH
		HDISH.setCONO(XXCONO);
		HDISH.setINOU(1);
		HDISH.setDLIX(dlix);
		if (!HDISH.CHAIN("00", HDISH.getKey("00"))) {
			MICommon.setError( "", "WDL0203", MICommon.toAlpha(HDISH.getDLIX())); 		//A	XPRD0000508 170920
			return; 		//A	XPRD0000508 170920
		}
		//read MHPICH
		HPICH.setCONO(XXCONO);
		HPICH.setDLIX(dlix);
		HPICH.setPLSX(plsx);
		HPICH.CHAIN("00", HPICH.getKey("00"));
		//read mittra
		ITTRA.setCONO(XXCONO);
		ITTRA.setRIDI(dlix);
		ITTRA.setPLSX(plsx);
		ITTRA.setWHLO().moveLeftPad(HDISH.getWHLO());

		if (HDISH.getTTYP() == 51 && HPICH.getPISS().EQ("70")) {
			ITTRA.setTTYP(52);
		}
		//   Read records
		ITTRA.SETLL("70", ITTRA.getKey("70", 3));
		IN93 = !ITTRA.READE("70", ITTRA.getKey("70", 3));
		while(!IN93 ) {
			DSWHSL.move(ITTRA.getWHSL());
			if (ITTRA.getTTYP() == 52 &&
					DSSLTP.NE("=>")) {
				XXWHSL.move(ITTRA.getWHSL());
				XXTRQT = -(ITTRA.getTRQT());
				//} 		//D	JNS01202 170322

				HDISL.setCONO(XXCONO);
				HDISL.setDLIX(dlix);
				HDISL.setRORC(5);
				HDISL.setRIDN().moveLeftPad(ITTRA.getRIDN());
				HDISL.setRIDL(ITTRA.getRIDL());
				HDISL.setRIDX(ITTRA.getRIDX());
				HDISL.SETLL("00", HDISL.getKey("00"));
				IN94 = !HDISL.CHAIN("00", HDISL.getKey("00"));
				if(!IN94){
					long DSPS0RPQT = 0l;
					ITMAS.setCONO(HDISL.getCONO());
					ITMAS.setITNO().move(HDISL.getITNO());
					IN91 = !ITMAS.CHAIN("00", ITMAS.getKey("00"));
					if (LDAZD.REPF == 1) {
						RITDS();
					}
					//   Check Catch weight
					ITAUN.setDCCD(ITMAS.getDCCD());
					if (ITMAS.getCAWP() == 0) { 	
						if (ITMAS.getPPUN().NE(ITMAS.getUNMS())) {
							ITAUN.setCONO(ITMAS.getCONO());
							ITAUN.setITNO().move(ITMAS.getITNO());
							ITAUN.setAUTP(2);
							ITAUN.setALUN().move(ITMAS.getPPUN());
							IN91 = !ITAUN.CHAIN("00", ITAUN.getKey("00"));
						} 	
					} else { 	
						if (ITMAS.getCWUN().NE(ITMAS.getUNMS())) { 	
							ITAUN.setCONO(ITMAS.getCONO()); 	
							ITAUN.setITNO().move(ITMAS.getITNO()); 	
							ITAUN.setAUTP(1); 	
							ITAUN.setALUN().move(ITMAS.getCWUN()); 	
							IN91 = !ITAUN.CHAIN("00", ITAUN.getKey("00")); 	
						}	 	
					}
					XBCAWE = 0d;
					ITBAL.setCONO(HDISH.getCONO());
					ITBAL.setWHLO().move(HDISH.getWHLO());
					ITBAL.setITNO().move(HDISL.getITNO());
					IN91 = !ITBAL.CHAIN("00", ITBAL.getKey("00"));
					//   Retrieve MITLOC information
					ITLOC.setCONO(HDISH.getCONO());
					ITLOC.setWHLO().move(HDISH.getWHLO());
					ITLOC.setITNO().move(HDISL.getITNO());
					ITLOC.setWHSL().move(ITTRA.getWHSL());
					ITLOC.setBANO().move(ITTRA.getBANO());
					ITLOC.setCAMU().move(ITTRA.getCAMU());
					ITLOC.setREPN(ITTRA.getREPN());
					IN92 = !ITLOC.CHAIN("00", ITLOC.getKey("00"));
					if(IN92) {
						ITLOC.setSTAS('2');
						ITLOC.setPRDT(0);
						//   Retrive Lot ref
						MM428.setTTYP(ITTRA.getTTYP());
						MM428.setRIDN().move(ITTRA.getRIDN());
						MM428.setRIDL(ITTRA.getRIDL());
						MM428.setRIDX(ITTRA.getRIDX());
						MM428.setSEQU(1);
						MM428.setRIDI(ITTRA.getRIDI());
						MM428.setITNO().move(ITTRA.getITNO());
						MM428.setRGDT(ITTRA.getRGDT());
						MM428.setRGTM(ITTRA.getRGTM());
						MM428.setTMSX(ITTRA.getTMSX());
						MM428.setWHSL().move(ITTRA.getWHSL());
						MM428.setBANO().move(ITTRA.getBANO());
						MM428.setCAMU().move(ITTRA.getCAMU());
						MM428.setREPN(ITTRA.getREPN());
						IN91 = !MM428.CHAIN("00", MM428.getKey("00"));
						if (!IN91) {
							ITLOC.setBREF().move(MM428.getBREF());
							ITLOC.setBRE2().move(MM428.getBRE2());
							if (MM428.getSTAS() == '1' ||
									MM428.getSTAS() == '2' ||
									MM428.getSTAS() == '3') {
								ITLOC.setSTAS(MM428.getSTAS());
							}
						}
					}
					XXDTN.move(HDISL.getTRQT() - 0);
					//   Reverse order
					RRVS();
					if (toBoolean(PXIN60.getChar())) {
						this.MSGID.move(PXMSID);   		//A	XPRD0000508 170919
						MICommon.setError("NOK",this.MSGID.toString()); 		//A	XPRD0000508 170919
						return;
					}
					//   Save order numbers for later update
					if (XXIFCA == 1 &&
							MM428.getTTYP() == 31 ||
							XXIFCA == 0 &&
							MM428.getTTYP() == 31 &&
							XXKIT) {
						I2 = 1;
						I2 = lookUpEQ(RID, I2 - 1, HDISH.getRIDN());
						if (I2 >= 0) {
							IN92 = true;
							I2++;
						} else {
							IN92 = false;
							I2 = -I2;
						}
						if (!IN92) {
							I1++;
							moveToArray(RID, I1 - 1, HDISH.getRIDN());
						}
					}
					//   Update rental agreement
					if (GLINE.getRORC() == 0 && !GLINE.getRORN().isBlank()) {
						CRTLID();
					}
				}//chain MHDISL


			} 
			IN93 = !ITTRA.READE("70", ITTRA.getKey("70", 3));
		} 
		/*		while (!IN93) { 		
			DSWHSL.move(ITTRA.getWHSL());
			if (ITTRA.getTTYP() == 52 &&
					DSSLTP.NE("=>") ||
					ITTRA.getTTYP() != 52) {

				XXWHSL.move(ITTRA.getWHSL());
				XXTRQT = -(ITTRA.getTRQT());
				//} 		//D	JNS01202 170322

				HDISL.setCONO(XXCONO);
				HDISL.setDLIX(dlix);
				HDISL.setRORC(5);
				HDISL.setRIDN().moveLeftPad(ITTRA.getRIDN());
				HDISL.setRIDL(ITTRA.getRIDL());
				HDISL.setRIDX(ITTRA.getRIDX());
				HDISL.SETLL("00", HDISL.getKey("00"));
				IN94 = !HDISL.CHAIN("00", HDISL.getKey("00"));
				if(!IN94){
					long DSPS0RPQT = 0l;
					ITMAS.setCONO(HDISL.getCONO());
					ITMAS.setITNO().move(HDISL.getITNO());
					IN91 = !ITMAS.CHAIN("00", ITMAS.getKey("00"));
					if (LDAZD.REPF == 1) {
						RITDS();
					}
					//   Check Catch weight
					ITAUN.setDCCD(ITMAS.getDCCD());
					if (ITMAS.getCAWP() == 0) { 	
						if (ITMAS.getPPUN().NE(ITMAS.getUNMS())) {
							ITAUN.setCONO(ITMAS.getCONO());
							ITAUN.setITNO().move(ITMAS.getITNO());
							ITAUN.setAUTP(2);
							ITAUN.setALUN().move(ITMAS.getPPUN());
							IN91 = !ITAUN.CHAIN("00", ITAUN.getKey("00"));
						} 	
					} else { 	
						if (ITMAS.getCWUN().NE(ITMAS.getUNMS())) { 	
							ITAUN.setCONO(ITMAS.getCONO()); 	
							ITAUN.setITNO().move(ITMAS.getITNO()); 	
							ITAUN.setAUTP(1); 	
							ITAUN.setALUN().move(ITMAS.getCWUN()); 	
							IN91 = !ITAUN.CHAIN("00", ITAUN.getKey("00")); 	
						}	 	
					}
					XBCAWE = 0d;
					ITBAL.setCONO(HDISH.getCONO());
					ITBAL.setWHLO().move(HDISH.getWHLO());
					ITBAL.setITNO().move(HDISL.getITNO());
					IN91 = !ITBAL.CHAIN("00", ITBAL.getKey("00"));
					//   Retrieve MITLOC information
					ITLOC.setCONO(HDISH.getCONO());
					ITLOC.setWHLO().move(HDISH.getWHLO());
					ITLOC.setITNO().move(HDISL.getITNO());
					ITLOC.setWHSL().move(ITTRA.getWHSL());
					ITLOC.setBANO().move(ITTRA.getBANO());
					ITLOC.setCAMU().move(ITTRA.getCAMU());
					ITLOC.setREPN(ITTRA.getREPN());
					IN92 = !ITLOC.CHAIN("00", ITLOC.getKey("00"));
					if(IN92) {
						ITLOC.setSTAS('2');
						ITLOC.setPRDT(0);
						//   Retrive Lot ref
						MM428.setTTYP(ITTRA.getTTYP());
						MM428.setRIDN().move(ITTRA.getRIDN());
						MM428.setRIDL(ITTRA.getRIDL());
						MM428.setRIDX(ITTRA.getRIDX());
						MM428.setSEQU(1);
						MM428.setRIDI(ITTRA.getRIDI());
						MM428.setITNO().move(ITTRA.getITNO());
						MM428.setRGDT(ITTRA.getRGDT());
						MM428.setRGTM(ITTRA.getRGTM());
						MM428.setTMSX(ITTRA.getTMSX());
						MM428.setWHSL().move(ITTRA.getWHSL());
						MM428.setBANO().move(ITTRA.getBANO());
						MM428.setCAMU().move(ITTRA.getCAMU());
						MM428.setREPN(ITTRA.getREPN());
						IN91 = !MM428.CHAIN("00", MM428.getKey("00"));
						if (!IN91) {
							ITLOC.setBREF().move(MM428.getBREF());
							ITLOC.setBRE2().move(MM428.getBRE2());
							if (MM428.getSTAS() == '1' ||
									MM428.getSTAS() == '2' ||
									MM428.getSTAS() == '3') {
								ITLOC.setSTAS(MM428.getSTAS());
							}
						}
					}
					XXDTN.move(HDISL.getTRQT() - 0);
					if(ITTRA.getTTYP()==51){ 		//A	JNS01204 170406
						ITTRA.setTTYP(52); 		//A	JNS01204 170406
					} 		//A	JNS01204 170406
					//   Reverse order
					RRVS();
					if (toBoolean(PXIN60.getChar())) {
						return;
					}
					//   Save order numbers for later update
					if (XXIFCA == 1 &&
							MM428.getTTYP() == 31 ||
							XXIFCA == 0 &&
							MM428.getTTYP() == 31 &&
							XXKIT) {
						I2 = 1;
						I2 = lookUpEQ(RID, I2 - 1, HDISH.getRIDN());
						if (I2 >= 0) {
							IN92 = true;
							I2++;
						} else {
							IN92 = false;
							I2 = -I2;
						}
						if (!IN92) {
							I1++;
							moveToArray(RID, I1 - 1, HDISH.getRIDN());
						}
					}
					//   Update rental agreement
					if (GLINE.getRORC() == 0 && !GLINE.getRORN().isBlank()) {
						CRTLID();
					}
				}//chain MHDISL
			} 		//A	JNS01202 170322
			IN93 = !ITTRA.READE("70", ITTRA.getKey("70", 4));
		}*/
		if (updateOOI950) { 	
			UPDDOC(); 	
			R950(); 	
		}
		if (HDISH.getRORC() == 7) {
			CUORL.setCONO(HDISH.getCONO());
			CUORL.setORNO().move(ITTRA.getRORN());
			CUORL.setPONR(ITTRA.getRORL());
			if (CUORL.CHAIN_LOCK("00", CUORL.getKey("00"))) {
				if (CUORL.getAOST().EQ("40")) {
					CUORL.setAOST().move("30");
				}
				CUORL.UPDAT("00");
			} else {
				CUORL.UNLOCK("00");
			}
			CUORH.setCONO(HDISH.getCONO());
			CUORH.setORNO().move(ITTRA.getRORN());            
			if (CUORH.CHAIN_LOCK("00", CUORH.getKey("00"))) {
				if (CUORH.getAOSL().GT("30")) {
					CUORH.setAOSL().move("30");
				}
				if (CUORH.getAOST().LT("30")) {
					CUORH.setAOST().move("30");
				}
				CUORH.UPDAT("00");
			} else {
				CUORH.UNLOCK("00");
			}
		}
		//   - Check if non stocked item or Kit exist
		if (XXIFCA == 1 && MM428.getTTYP() == 31) {
			if (HDISH.getRIDN().isBlank()) {
				RCHNS();
			}
			CLIN();
		} else {
			reverseNonStockItm();
		}
		//   - Check if Kit detail changes
		if (XXIFCA == 0 && MM428.getTTYP() == 31 && XXKIT) {
			UKIT();
		}
		if (!toBoolean(PXIN60.getChar())) { 		//A	XPRD0000508 170919
			MSGSTS.moveLeft("OK"); 		//A	XPRD0000508 170919
		} else { 		//A	XPRD0000508 170919
			this.MSGID.move(PXMSID);   		//A	XPRD0000508 170919
			MICommon.setError("NOK",this.MSGID.toString()); 		//A	XPRD0000508 170919
			return;
		} 		//A	XPRD0000508 170919
	}
	/**
	 *    RDOREV  - Execute command Create DO Reversal
	 */
	public void RDOREC() {       		//A	JNS01203 170328
		sCRCU20MIRCrtReversePick inRevPick = (sCRCU20MIRCrtReversePick)MICommon.getInDS(sCRCU20MIRCrtReversePick.class);
		inRevPick.set().moveLeftPad(MICommon.getData());
		//   Convert company to numeric
		if (MICommon.toNumericCompany(inRevPick.getQ0CONO())) {
			XXCONO = MICommon.getInt();
		} else {
			MICommon.setError("CONO");
			return;
		}
		int plsx = 0; 		
		long dlix = 0;
		if(!inRevPick.getQ0PLSX().toString().trim().isEmpty()) 		
			plsx = Integer.parseInt(inRevPick.getQ0PLSX().toString().trim()); 		
		if(!inRevPick.getQ0DLIX().toString().trim().isEmpty()) 	
			dlix = Long.parseLong(inRevPick.getQ0DLIX().toString().trim());
		if(!inRevPick.getQ0WHLO().toString().trim().isEmpty()) 	
			XXWHLO.moveLeftPad(inRevPick.getQ0WHLO());
		if (inRevPick.getQ0DLIX().isBlank()){ 		//A	XPRD0000508 170920
			MICommon.setError("DLIX", "WDL0202"); 		//A	XPRD0000508 170920
			return; 		//A	XPRD0000508 170920
		} 		//A	XPRD0000508 170920
		//read MHDISH
		HDISH.setCONO(XXCONO);
		HDISH.setINOU(1);
		HDISH.setDLIX(dlix);
		HDISH.setWHLO().moveLeft(XXWHLO);
		if (!HDISH.CHAIN("90", HDISH.getKey("90"))) {
			HDISH.setCONO(XXCONO);
			HDISH.setINOU(2);
			HDISH.setDLIX(dlix);
			HDISH.setWHLO().moveLeft(XXWHLO);
			//HDISH.CHAIN("90", HDISH.getKey("90")); 		//D	XPRD0000508 170920
			if (!HDISH.CHAIN("00", HDISH.getKey("00"))) { 		//A	XPRD0000508 170920
				MICommon.setError( "", "WDL0203", MICommon.toAlpha(HDISH.getDLIX())); 		//A	XPRD0000508 170920
				return; 		//A	XPRD0000508 170920
			} 		//A	XPRD0000508 170920
		}

		//read MHPICH
		HPICH.setCONO(XXCONO);
		HPICH.setDLIX(dlix);
		HPICH.setPLSX(plsx);
		HPICH.CHAIN("00", HPICH.getKey("00"));
		MMMNGRORDS.setMMMNGRORDS().clear();
		// check for goods responsible   
		if (HDISH.getRORC() == 4 || HDISH.getRORC() == 5) {      
			GHEAD.setCONO(HDISH.getCONO());     
			if (HDISH.getRIDN().isBlank()) {       
				HDISL.setCONO(HDISH.getCONO());       
				HDISL.setDLIX(HDISH.getDLIX());       
				HDISL.SETLL("00", HDISL.getKey("00", 2));      
				if (HDISL.READE("00", HDISL.getKey("00", 2))) {      
					GHEAD.setTRNR().move(HDISL.getRIDN());      
				}       
			} else {       
				GHEAD.setTRNR().move(HDISH.getRIDN());       
			}     
			if (GHEAD.CHAIN("00", GHEAD.getKey("00"))) {    
				//  Find terms of delivery in CSYTAB      
				if (baseForCorrectedInvoice() == 2||   
						baseForCorrectedInvoice() == 3 ){      
					MMMNGRORDS.setQ0BFCI(1);      
				}        
			}  
		}          
		MMMNGRORDS.setQ0CONO(HDISH.getCONO());
		MMMNGRORDS.setQ0WHLO().move(HDISH.getWHLO());
		MMMNGRORDS.setQ0RIDI(HDISH.getDLIX());
		MMMNGRORDS.setQ0TRDT(this.CUDATE);
		MMMNGRORDS.setQ0TRTM(movexTime());
		MMMNGRORDS.setQ0RESP().move(this.DSUSS);
		PXENV.move(toChar(false));
		PXCHID.move(this.DSUSS);
		PXOPC.moveLeftPad("*RVS");
		rMNGRORpreCall();
		apCall("MMMNGROR", rMNGROR);
		rMNGRORpostCall();
		IN60 = toBoolean(PXIN60.getChar());
		//if (toBoolean(PXIN60.getChar())) { 		//D	XPRD0000508 170919
		//return; 		//D	XPRD0000508 170919
		//} 		//D	XPRD0000508 170919
		this.MSGDTA.clear();          		//A	XPRD0000508 170919
		this.MSGDTA.move(PXMSGD);     		//A	XPRD0000508 170919
		if (!toBoolean(PXIN60.getChar())) { 		//A	XPRD0000508 170919
			MSGSTS.moveLeft("OK"); 		//A	XPRD0000508 170919
		} else { 		//A	XPRD0000508 170919
			this.MSGID.move(PXMSID);   		//A	XPRD0000508 170919
			MICommon.setError("NOK",this.MSGID.toString()); 		//A	XPRD0000508 170919
			return;
		} 		//A	XPRD0000508 170919

	}

	/**
	 *    RDOREV  - Execute command Delete DO
	 */
	public void RDODEL() {      
		sCRCU20MIRDltDO inDltDO = (sCRCU20MIRDltDO)MICommon.getInDS(sCRCU20MIRDltDO.class);
		inDltDO.set().moveLeftPad(MICommon.getData());
		//   Convert company to numeric
		if (MICommon.toNumericCompany(inDltDO.getQ0CONO())) {
			XXCONO = MICommon.getInt();
		} else {
			MICommon.setError("CONO");
			return;
		}
		if (inDltDO.getQ0TRNR().isBlank()){
			MICommon.setError( "TRNR"); 		//A	XPRD0000508 170920
			return;
		}
		GHEAD.setCONO(XXCONO);
		GHEAD.setTRNR().move(inDltDO.getQ0TRNR());
		//   Check record
		IN91 = !GHEAD.CHAIN("00", GHEAD.getKey("00"));
		IN92 = GHEAD.getErr("00");
		if (IN91) {
			MICommon.setError( "", "WTR0901", inDltDO.getQ0TRNR()); 		//A	XPRD0000508 170920
			return; 		//A	XPRD0000508 170920
		}
		MMS100DS.setBXOPT2().moveLeftPad("*DLT");
		clear(BEX);
		loop1:
			for (FL = 1; FL <= 299; FL++) {
				if (BFD[FL - 1].EQ("/END/")) {
					break loop1;
				}
				BEX[FL - 1].moveLeft(BFD[FL - 1]);
			}
		if (FL > 299L) {
			FL = 299;// last val for index
		}
		//    Delete Attribute
		GLINE.setCONO(LDAZD.CONO);
		GLINE.setTRNR().moveLeftPad(GHEAD.getTRNR());
		GLINE.SETLL("00", GLINE.getKey("00", 2));
		IN93 = !GLINE.READE("00", GLINE.getKey("00", 2));
		// Do not delete if the order status is 99 and if DO line exists
		int stsh=Integer.valueOf(GHEAD.getTRSH().toString().substring(1));
		int stsl=Integer.valueOf(GHEAD.getTRSL().toString().substring(1));
		if (stsh>3 && stsl>3) { 		//A	XPRD0000511 171003
			if (!IN93) { 		//A	XPRD0000511 171003
				MICommon.setError( "", "LT45116", inDltDO.getQ0TRNR()); 		 		//A	XPRD0000511 171003
				return; 		//A	XPRD0000511 171003 
			} 		//A	XPRD0000511 171003
		} 		//A	XPRD0000511 171003
		while (!IN93) {
			if (GLINE.getATNR() != 0L) {
				ATMNGATRDS.setATMNGATRDS().clear();
				ATMNGATRDS.setA1CONO(GLINE.getCONO());
				ATMNGATRDS.setA1ITNO().move(GLINE.getITNO());
				ATMNGATRDS.setA1ATNR(GLINE.getATNR());
				ATMNGATRDS.setA1ORCA().move("501");
				if (GTYPE.getTTYP() >= 40 && GTYPE.getTTYP() <= 49) {
					ATMNGATRDS.setA1ORCA().moveLeft(GTYPE.getTTYP(), 2);
					ATMNGATRDS.setA1ORCA().moveRight("1");
				}
				PXOPC.moveLeftPad("*DLT");
				PXENV.move(toChar(false));
				rMNGATRpreCall();
				apCall("ATMNGATR", rMNGATR);
				rMNGATRpostCall();
			}
			IN93 = !GLINE.READE("00", GLINE.getKey("00", 2));
		}
		MDFDS();
		//   CALL=MMS100BE Req/Distr Order. Open
		rMMS100preCall();
		apCall("MMS100BE", rMMS100);
		rMMS100postCall();
		//   If error
		if (MMS100DS.getBXIN60() == toChar(true)) {
			FL = 1;
			FL = lookUpEQ(BEX, FL - 1, MMS100DS.getBXFLDI());
			if (FL >= 0) {
				IN93 = true;
				FL++;
			} else {
				IN93 = false;
				FL = -FL;
			}
			IN60 = true;
		}
		if(IN60) {
			PXIN60.move('1');
		} else {
			PXIN60.move('0');
		}
		if (!toBoolean(PXIN60.getChar())) { 		//A	XPRD0000508 170919
			MSGSTS.moveLeft("OK"); 		//A	XPRD0000508 170919
		} else { 		//A	XPRD0000508 170919
			this.MSGID.move(PXMSID);   		//A	XPRD0000508 170919
			MICommon.setError("NOK",formatToString(MMS100DS.getBXMSGD())); 		//A	XPRD0000508 170919
			return;
		} 		//A	XPRD0000508 170919
	}
	
	/**
	 *    PICKREVNS  - Execute command PICK REVERSE NON STOCK ITEM
	 */
	public void PICKREVNS() {    		//A	XPRD00742 201222
		sCRCU20MIRCrtReversePick inRevPickNS = (sCRCU20MIRCrtReversePick)MICommon.getInDS(sCRCU20MIRCrtReversePick.class);
		inRevPickNS.set().moveLeftPad(MICommon.getData());
		//   Convert company to numeric
		if (MICommon.toNumericCompany(inRevPickNS.getQ0CONO())) {
			XXCONO = MICommon.getInt();
		} else {
			MICommon.setError("CONO");
			return;
		}
		int plsx = 0; 		
		long dlix = 0; 		
		if(!inRevPickNS.getQ0PLSX().toString().trim().isEmpty()) 		
			plsx = Integer.parseInt(inRevPickNS.getQ0PLSX().toString().trim()); 		
		if(!inRevPickNS.getQ0DLIX().toString().trim().isEmpty()) 		
			dlix = Long.parseLong(inRevPickNS.getQ0DLIX().toString().trim()); 		
		if(!inRevPickNS.getQ0WHLO().toString().trim().isEmpty()) 	
			XXWHLO.moveLeftPad(inRevPickNS.getQ0WHLO());
		//   Check Delivery number / Order index if blank
		//   MSGID=WDL0202 Delivery number must be entered
		if (inRevPickNS.getQ0DLIX().isBlank()){ 		
			MICommon.setError("DLIX", "WDL0202"); 		
			return; 		
		} 	
		//read MHPICH
				HDISH.setCONO(XXCONO);
				HDISH.setINOU(2);
				HDISH.setPLSX(plsx);
				HDISH.setWHLO().moveLeft(XXWHLO);
				HDISH.CHAIN("00", HDISH.getKey("00"));
				
				if (HDISH.getTTYP() == 51 && HDISH.getPGRS().EQ("70")) {
			//		we need to update MHPICH PISS = 70 based on data from M
				}
		HDISH.setCONO(LDAZD.CONO);
		HDISH.setINOU(2);
		HDISH.setDLIX(dlix);
		if(HDISH.CHAIN_LOCK("00", HDISH.getKey("00"))){
			if(HDISH.getPGRS().EQ("70")){
				HDISL.setCONO(HDISH.getCONO());
				HDISL.setDLIX(HDISH.getDLIX());
				HDISL.setRORC(HDISH.getRORC());
				HDISL.setRIDN().moveLeftPad(HDISH.getRIDN());
				HDISL.SETLL("00", HDISL.getKey("00", 4));
				IN93 = !HDISL.READE_LOCK("00", HDISL.getKey("00", 4));
				while(!IN93){
					
					ITMAS.setCONO(HDISL.getCONO());
					ITMAS.setITNO().moveLeftPad(HDISL.getITNO());
					if(ITMAS.CHAIN("00", ITMAS.getKey("00"))){
						if(ITMAS.getSTCD()== 0 ){
							HPICH.setCONO(HDISH.getCONO());
							HPICH.setDLIX(HDISL.getDLIX());
							HPICH.setPLSX(plsx);
							if(HPICH.CHAIN_LOCK("00", HPICH.getKey("00"))){
								if(HPICH.getPISS().EQ("90")){
									//HPICH.setPISS().moveLeftPad("70");
									
								}
								HPICH.DELET("00");
								HDISL.DELET("00");
								HDISH.DELET("00");
								
								GHEAD.setCONO(HDISL.getCONO());
								GHEAD.setTRNR().moveLeftPad(HDISL.getRIDN());
								if(GHEAD.CHAIN_LOCK("00", GHEAD.getKey("00"))){
									GHEAD.setTRSH().moveLeftPad("33");
									GHEAD.setTRSL().moveLeftPad("33");
									GHEAD.UPDAT("00");
									GLINE.setCONO(HDISL.getCONO());
									GLINE.setTRNR().moveLeftPad(HDISL.getRIDN());
									GLINE.SETLL("00", GLINE.getKey("00", 2));
									IN94 = !GLINE.READE_LOCK("00", GLINE.getKey("00", 2));
									while(!IN94){
										GLINE.setTRSH().moveLeftPad("33");
										GLINE.setALQT(GLINE.getQTIT());
										GLINE.setQTIT(0);
										
										GLINE.UPDAT("00");
										GLINE.UNLOCK("00");
										IN94 = !GLINE.READE_LOCK("00", GLINE.getKey("00", 2));
									}
								}
							}
						}
					}
					HDISL.UNLOCK("00");
					IN93 = !HDISL.READE_LOCK("00", HDISL.getKey("00", 4));
				}
			}
			HDISH.UNLOCK("00");
		}
		
		
	} 		//A	XPRD00742 201222
	
	
	/**
	 *    UKIT - Update detailed Kit changes
	 */
	public void UKIT() {
		for (I2 = 1; I2 <= I1; I2++) {
			if (!RID[I2 - 1].isBlank()) {
				//   Clear parameter list
				MMMNGROSDS.setMMMNGROSDS().clear();
				MMMNGROSDS.setM8CONO(HDISH.getCONO());
				MMMNGROSDS.setM8RIDN().moveRight(RID[I2 - 1]);
				MMMNGROSDS.setM8RIDI(ITTRA.getRIDI());
				MMMNGROSDS.setM8RIDL(ITTRA.getRIDL()); 	
				MMMNGROSDS.setM8RIDX(ITTRA.getRIDX()); 	
				MMMNGROSDS.setM8WHLO().move(ITTRA.getWHLO());
				MMMNGROSDS.setM8PGNM().moveLeft("MMS428");
				PXENV.move(toChar(false));
				PXOPC.moveLeftPad("*KIT");
				rMNGROSpreCall();
				apCall("MMMNGROS", rMNGROS);
				rMNGROSpostCall();
			}
		}
		if (I2 > I1) {
			I2 = I1;// last val for index
		}
	}


	/**
	 *    reverseNonStockItm - Reverse non stock item for TTYP 41 and 51
	 */
	public void reverseNonStockItm() {
		if (MM428.getTTYP() == 52) {
			ITALO.setCONO(HDISH.getCONO());
			ITALO.setTTYP(51);
			ITALO.setRIDN().moveLeftPad(HDISH.getRIDN());
			ITALO.setRIDO(0);
			ITALO.setRIDI(HDISH.getDLIX());
			ITALO.setWHLO().moveLeft(HDISH.getWHLO());
			ITALO.setPLSX(MM428.getPLSX());
			ITALO.SETLL("20", ITALO.getKey("20", 7));
			while (ITALO.READE("20", ITALO.getKey("20", 7))) {
				if (ITALO.getSTCD() == 0) {
					RRVS();
				}
			}
		} 
		else if (MM428.getTTYP() == 41) {
			HDISL.setCONO(HDISH.getCONO());
			HDISL.setRORC(HDISH.getRORC());
			HDISL.setRIDN().moveLeftPad(HDISH.getRIDN());
			HDISL.setDLIX(HDISH.getDLIX());
			HDISL.SETLL("20", HDISL.getKey("20", 4));
			while (HDISL.READE("20", HDISL.getKey("20", 4))) {
				if (HDISL.getSTCD() == 0) {
					RRVS();
				}
			}
		}   
	}

	/**
	 *    UPDDOC - Update documents per delivery
	 */
	public void UPDDOC() { 	
		XFDOCD = 0; 	
		XFDOC2 = 0; 	
		XFDOC3 = 0; 	
		XFDOC4 = 0; 	
		ODOCU.setCONO(DHEAD.getCONO()); 	
		ODOCU.setORNO().move(DHEAD.getORNO()); 	
		//   Order documents 	
		IN91 = !ODOCU.CHAIN("00", ODOCU.getKey("00", 2)); 	
		while (!IN91) { 	
			DEDOC.setCONO(ODOCU.getCONO()); 	
			DEDOC.setDONR().move(ODOCU.getDONR()); 	
			DEDOC.setDOVA().move(ODOCU.getDOVA()); 	
			IN91 = !DEDOC.CHAIN("00", DEDOC.getKey("00")); 	
			if (!IN91 && 	
					DEDOC.getDOCL() >= '1' && 	
					DEDOC.getDOCL() <= '3' && 	
					ODOCU.getDONR().NE("325") || 	
					!IN91 && 	
					DEDOC.getDOCL() >= '1' && 	
					DEDOC.getDOCL() <= '3' && 	
					ODOCU.getDONR().EQ("325") && 	
					CRS721DS.getP4PRBA() == 2) { 	
				DDOCU.setCONO(ODOCU.getCONO()); 	
				DDOCU.setORNO().move(ODOCU.getORNO()); 	
				DDOCU.setWHLO().move(DHEAD.getWHLO()); 	
				DDOCU.setDONR().move(ODOCU.getDONR()); 	
				IN91 = !DDOCU.CHAIN_LOCK("00", DDOCU.getKey("00", 5)); 	
				IN92 = DDOCU.getErr("00"); 	
				if (IN91) { 	
					DDOCU.clearNOKEY("00"); 	
					DDOCU.setDOVA().move(ODOCU.getDOVA()); 	
					DDOCU.setDOTP(ODOCU.getDOTP()); 	
					DDOCU.setNOEX(ODOCU.getNOEX()); 	
					DDOCU.setDOCD(ODOCU.getDOCD()); 	
					DDOCU.setDOVA().move(ODOCU.getDOVA()); 	
					if (DDOCU.getDONR().EQ("380")) { 	
						XFDOCD = DDOCU.getDOCD(); 	
					} 	
					if (DDOCU.getDONR().EQ("270")) { 	
						XFDOC2 = DDOCU.getDOCD(); 	
					} 	
					if (DDOCU.getDONR().EQ("325")) { 	
						XFDOC3 = DDOCU.getDOCD(); 	
					} 	
					if (DDOCU.getDONR().EQ("350")) { 	
						XFDOC4 = DDOCU.getDOCD(); 	
					} 	
					DDOCU.setRGDT(DHEAD.getLMDT()); 	
					DDOCU.setRGTM(movexTime()); 	
					IN92 = !DDOCU.WRITE_CHK("00"); 	
				} else { 	
					DDOCU.UPDAT("00"); 	
					if (DDOCU.getDONR().EQ("380")) { 	
						XFDOCD = DDOCU.getDOCD(); 	
					} 	
					if (DDOCU.getDONR().EQ("270")) { 	
						XFDOC2 = DDOCU.getDOCD(); 	
					} 	
					if (DDOCU.getDONR().EQ("325")) { 	
						XFDOC3 = DDOCU.getDOCD(); 	
					} 	
					if (DDOCU.getDONR().EQ("350")) { 	
						XFDOC4 = DDOCU.getDOCD(); 	
					} 	
				} 	
			} 	
			IN91 = !ODOCU.READE("00", ODOCU.getKey("00", 2)); 	
		} 	
	}

	/**
	 *    R950 - Update autojob file    OIS950
	 */
	public void R950() { 	
		//   Init lda values for OIS950 	
		RLDA(); 	
		if (HDISH.getTTYP() == 31) {
			OHEAD.setCONO(MM428.getCONO());
			OHEAD.setORNO().move(MM428.getRIDN());
			OHEAD.CHAIN("00", OHEAD.getKey("00"));
			OTYPE.setCONO(OHEAD.getCONO());
			OTYPE.setORTP().move(OHEAD.getORTP());
			OTYPE.CHAIN("00", OTYPE.getKey("00"));
		}
		OI950.setCONO(DHEAD.getCONO()); 	
		OI950.setDIVI().move(DHEAD.getDIVI()); 	
		OI950.setORNO().move(DHEAD.getORNO()); 	
		OI950.setDLIX(DHEAD.getDLIX()); 	
		OI950.setTEPY().move(DHEAD.getTEPY());  	
		OI950.setWHLO().move(DHEAD.getWHLO()); 	
		if (OTYPE.getIVLV() == 3 || XFDOCD == 1) { 	
			OI950.setNEXT(4); 	
		} else { 	
			OI950.setNEXT(3); 	
		} 	
		OI950.setNEXX(0); 	
		if (XFDOC2 == 0 && XFDOC3 == 0 && XFDOC4 == 0) { 	
			OI950.setNEX1(4); 	
		} else { 	
			OI950.setNEX1(3); 	
		} 	
		OI950.setPGNM().moveLeft("OIS900"); 	
		OI950.setRGDT(this.CUDATE); 	
		OI950.setRGTM(movexTime()); 	
		OI950.setCHID().move(this.DSUSS); 	
		OI950.WRITE("00"); 	
	} 

	/**
	 *    RLDA - Init lda values for OIS950
	 */
	public void RLDA() { 	
		MNDIV.setCONO(DHEAD.getCONO()); 	
		MNDIV.setDIVI().move(DHEAD.getDIVI()); 	
		MNDIV.CHAIN("00", MNDIV.getKey("00")); 	
		MNCMP.setCONO(DHEAD.getCONO()); 	
		MNCMP.CHAIN("00", MNCMP.getKey("00")); 	
		OI950.setZDLCDC(LDAZD.LCDC); 	
		OI950.setZDMXMS(LDAZD.MXMS); 	
		OI950.setZDCONO(MNDIV.getCONO()); 	
		OI950.setZDMXDR(MNDIV.getMXDR()); 	
		OI950.setZDTTBL().move(LDAZD.TTBL); 	
		OI950.setZDSTTK().move(LDAZD.STTK); 	
		OI950.setZDSTRT().move(LDAZD.STRT); 	
		OI950.setZDRESP().move(DHEAD.getCHID()); 	
		OI950.setZDREPF(LDAZD.REPF); 	
		OI950.setZDLANC().move(LDAZD.LANC); 	
		OI950.setZDDMCU(MNDIV.getDMCU()); 	
		OI950.setZDCMTP(MNCMP.getCMTP()); 	
		OI950.setZDDTFM().move(MNDIV.getDTFM()); 	
		OI950.setZDAUPF().move(LDAZD.AUPF); 	
		OI950.setZDLOCD().move(MNDIV.getLOCD()); 	
		OI950.setZDAUFI().move(LDAZD.AUFI); 	
		OI950.setZDMUNI(LDAZD.MUNI); 	
		OI950.setZDCDCD(MNDIV.getCDCD()); 	
		OI950.setZDDCFM(LDAZD.DCFM); 	
		OI950.setZDPCPA(LDAZD.PCPA); 	
		OI950.setZDDIVI().move(MNDIV.getDIVI()); 	
		OI950.setZDROW3().move(MNDIV.getROW3()); 	
		OI950.setZDACMT(LDAZD.ACMT); 	
	} 	

	public void CRTLID() {
		STMNGLIDDS.setSTMNGLIDDS().clear();
		PXOPC.clear();
		PXOPC.moveLeftPad("*DEL");
		GLINE.setCONO(HPICH.getCONO());
		GLINE.setTRNR().move(ITTRA.getRIDN());
		GLINE.setPONR(ITTRA.getRIDL());
		GLINE.setPOSX(ITTRA.getRIDX());
		IN91 = !GLINE.CHAIN("00", GLINE.getKey("00"));
		STMNGLIDDS.setP6CONO(GLINE.getCONO());
		STMNGLIDDS.setP6RIDN().move(GLINE.getRORN());
		STMNGLIDDS.setP6RIDL(GLINE.getRORL());
		STMNGLIDDS.setP6RIDX(GLINE.getRORX());
		STMNGLIDDS.setP6RORN().move(MMMNGROSDS.getM8RIDN());
		STMNGLIDDS.setP6RORL(MMMNGROSDS.getM8RIDL());
		STMNGLIDDS.setP6RORX(MMMNGROSDS.getM8RIDX());
		STMNGLIDDS.setP6DLIX(MMMNGROSDS.getM8RIDI());
		STMNGLIDDS.setP6FWHL().move(MMMNGROSDS.getM8WHLO());
		STMNGLIDDS.setP6ITNO().move(MMMNGROSDS.getM8ITNO());
		STMNGLIDDS.setP6BANO().move(MMMNGROSDS.getM8BANO());
		STMNGLIDDS.setP6TRDT(this.CUDATE);
		STMNGLIDDS.setP6TRTM(movexTime());
		STMNGLIDDS.setP6TRQT(ITTRA.getTRQT() - 0);
		STMNGLIDDS.setP6OEND(0);//MMMNGROSDS.getM8OEND());
		STMNGLIDDS.setP6SOQT(0d);
		STMNGLIDDS.setP6USQT(0d);
		STMNGLIDDS.setP6CHID().move(this.DSUSS);
		rMNGLIDpreCall();
		apCall("STMNGLID", rMNGLID);
		rMNGLIDpostCall();
	}

	/**
	 *    RCHNS - Update
	 */
	public void RCHNS() {
		HDISL.setCONO(HDISH.getCONO());
		HDISL.setDLIX(HDISH.getDLIX());
		HDISL.SETLL("00", HDISL.getKey("00", 2));
		IN93 = !HDISL.READE("00", HDISL.getKey("00", 2));
		while (!IN93) {
			if (HDISL.getSTCD() == 0) {
				I2 = 1;
				I2 = lookUpEQ(RID, I2 - 1, HDISL.getRIDN());
				if (I2 >= 0) {
					IN92 = true;
					I2++;
				} else {
					IN92 = false;
					I2 = -I2;
				}
				if (!IN92) {
					I1++;
					moveToArray(RID, I1 - 1, HDISL.getRIDN());
				}
			}
			IN93 = !HDISL.READE("00", HDISL.getKey("00", 2));
		}
	}

	/**
	 *    CLIN - Check Kit & non stocked items
	 */
	public void CLIN() {
		for (I2 = 1; I2 <= I1; I2++) {
			if (!RID[I2 - 1].isBlank()) {
				// Clear parameter list
				MMMNGROSDS.setMMMNGROSDS().clear();
				MMMNGROSDS.setM8CONO(HDISH.getCONO());
				MMMNGROSDS.setM8RIDI(ITTRA.getRIDI());
				MMMNGROSDS.setM8RIDN().moveRight(RID[I2 - 1]);
				MMMNGROSDS.setM8WHLO().move(ITTRA.getWHLO());
				MMMNGROSDS.setM8RESP().move(this.DSUSS);
				MMMNGROSDS.setM8TTYP(ITTRA.getTTYP());
				MMMNGROSDS.setM8PLRI().move(HPICH.getPLRI());
				MMMNGROSDS.setM8RPDT(XXRPDT);
				//   Print log
				if (MMS428DS.getM7PRLG() == 1) {
					MMMNGROSDS.setM8PRLG(1);
					MMMNGROSDS.setM8JNU(MMS428DS.getM7JNU());
					MMMNGROSDS.setM8PRD(MMS428DS.getM7PRD());
					MMMNGROSDS.setM8PRT(MMS428DS.getM7PRT());
					MMMNGROSDS.setM8INV(1);
				}
				MMMNGROSDS.setM8PGNM().moveLeft("MMS428");
				MMMNGROSDS.setM8IFCA(XXIFCA);
				PXENV.move(toChar(false));
				PXOPC.moveLeftPad("*LIN");
				rMNGROSpreCall();
				apCall("MMMNGROS", rMNGROS);
				rMNGROSpostCall();
			}
		}
		if (I2 > I1) {
			I2 = I1;// last val for index
		}
	}


	public void RRVS() {
		MMMNGROSDS.setMMMNGROSDS().clear();
		MMMNGROSDS.setM8CONO(HDISH.getCONO());
		MMMNGROSDS.setM8FACI().clear();		//move(DSP.S0FACI);
		MMMNGROSDS.setM8WHLO().move(ITTRA.getWHLO());
		MMMNGROSDS.setM8ITNO().move(ITTRA.getITNO());
		MMMNGROSDS.setM8RGDT(ITTRA.getRGDT());
		MMMNGROSDS.setM8RGTM(ITTRA.getRGTM());
		MMMNGROSDS.setM8TMSX(ITTRA.getTMSX());
		MMMNGROSDS.setM8RESP().move(this.DSUSS);
		MMMNGROSDS.setM8TTYP(ITTRA.getTTYP());
		//MMMNGROSDS.setM8WHSL().move(ITTRA.getWHSL()); 		//D	JNS01202 170322
		MMMNGROSDS.setM8WHSL().move(XXWHSL); 		//A	JNS01202 170322
		MMMNGROSDS.setM8BANO().move(ITTRA.getBANO());
		MMMNGROSDS.setM8ATNR(ITLOC.getATNR()); 	
		MMMNGROSDS.setM8CAMU().move(ITTRA.getCAMU());
		MMMNGROSDS.setM8BREF().move(ITLOC.getBREF());
		MMMNGROSDS.setM8BRE2().move(ITLOC.getBRE2());
		MMMNGROSDS.setM8RIDN().move(ITTRA.getRIDN());
		MMMNGROSDS.setM8RIDO(ITTRA.getRIDO());
		MMMNGROSDS.setM8RIDL(ITTRA.getRIDL());
		MMMNGROSDS.setM8RIDX(ITTRA.getRIDX());
		MMMNGROSDS.setM8RIDI(ITTRA.getRIDI());
		MMMNGROSDS.setM8PLSX(ITTRA.getPLSX());
		MMMNGROSDS.setM8RFTX().move(ITTRA.getRFTX());
		MMMNGROSDS.setM8REPN(ITTRA.getREPN());
		MMMNGROSDS.setM8PLRI().move(HPICH.getPLRI());
		MMMNGROSDS.setM8STAS(ITLOC.getSTAS());
		//	MMMNGROSDS.setM8TRQT(ITTRA.getTRQT()); 		//D	JNS01202 170322
		MMMNGROSDS.setM8TRQT(XXTRQT); 		//A	JNS01202 170322
		MMMNGROSDS.setM8RPQT(0);
		MMMNGROSDS.setM8CAWE(ITTRA.getCAWE());
		MMMNGROSDS.setM8CAWR(0);
		MMMNGROSDS.setM8OEND(0);
		MMMNGROSDS.setM8RPDT(movexDate());
		MMMNGROSDS.setM8PISS().move(HPICH.getPISS());
		MMMNGROSDS.setM8PRDT(ITLOC.getPRDT());
		//   Print log
		//			if (MMS428DS.getM7PRLG() == 1 || DSP.WWPRLG == 1) {
		//				MMMNGROSDS.setM8PRLG(1);
		//				MMMNGROSDS.setM8JNU(MMS428DS.getM7JNU());
		//				MMMNGROSDS.setM8PRD(MMS428DS.getM7PRD());
		//				MMMNGROSDS.setM8PRT(MMS428DS.getM7PRT());
		//				MMMNGROSDS.setM8INV(DSP.WSINV);
		//			}
		MMMNGROSDS.setM8PGNM().moveLeft("MMS428");
		MMMNGROSDS.setM8IFCA(1);
		MMMNGROSDS.setM8KIT('0');
		// Check if D0/RO transaction 	
		if (MMMNGROSDS.getM8TTYP() >= 40 && MMMNGROSDS.getM8TTYP() <= 52){ 	 	
			//  Find terms of delivery in CSYTAB 	
			if (baseForCorrectedInvoice() == 1){ 	
				MMMNGROSDS.setM8BFCI(1); 	
			}    	
		}
		//  Flag sublots
		if (ITMAS.getSUME() == 1) {
			assignSublots();
		}
		PXENV.move(toChar(false));
		PXOPC.moveLeftPad("*RVS");
		rMNGROSpreCall();
		apCall("MMMNGROS", rMNGROS);
		rMNGROSpostCall();
		XXKIT = toBoolean(MMMNGROSDS.getM8KIT());
		if (!PXMSID.isBlank() && PXMSID.NE("MMS473")) {
			//  Flag sublots
			if (ITMAS.getSUME() == 1) {
				reverseSublots();
			}
			return;
		}
		//if (PXMSID.EQ("MMS473")) { 		//D	JNS012 170219
		//RCHK(); 		//D	JNS012 170219
		//if (this.MSGID.NE("WDL0203")) { 		//D	JNS012 170219
		//LDAZZ.ORNO.moveLeft(ITTRA.getRIDN()); 		//D	JNS012 170219
		//LDAZZ.DLIX = ITTRA.getRIDI(); 		//D	JNS012 170219
		//LDAZZ.PONR = ITTRA.getRIDL(); 		//D	JNS012 170219
		//LDAZZ.POSX = ITTRA.getRIDX(); 		//D	JNS012 170219
		//LDAZZ.TDA1.moveLeftPad("REVERSE"); 		//D	JNS012 170219
		//LDAZZ.TDA2.move(ITTRA.getPLSX()); 		//D	JNS012 170219
		//LDAZZ.TDA4.moveLeft(ITTRA.getCAMU()); 		//D	JNS012 170219
		//LDAZZ.WHSL.moveLeft(ITTRA.getWHSL()); 		//D	JNS012 170219
		//LDAZZ.BANO.moveLeft(ITTRA.getBANO()); 		//D	JNS012 170219
		//MMMNGROSDS.setM8TRQT(ITTRA.getTRQT() - 0); 		//D	JNS012 170219
		//LDAZZ.TDA5.moveRightPad(MMMNGROSDS.getM8TRQT(), 15, 6); 		//D	JNS012 170219
		//MMMNGROSDS.setM8CAWR(ITTRA.getCAWE() - XBCAWE); 		//D	JNS012 170219
		//LDAZZ.TDA3.moveRightPad((MMMNGROSDS.getM8CAWR()/MMMNGROSDS.getM8TRQT()), 15, 6); 		//D	JNS012 170219
		//PXMSID.clear(); 		//D	JNS012 170219
		////RM473(); 		//D	JNS012 170219
		//updateOOI950 = true; 		//D	JNS012 170219
		//} 		//D	JNS012 170219
		//} 		//D	JNS012 170219
	}

	/**
	 *    RITDS  - Get Item description per user language
	 */
	public void RITDS() {
		CRRTVIDSDS.setCRRTVIDSDS().clear();
		CRRTVIDSDS.setI9CONO(ITMAS.getCONO());
		CRRTVIDSDS.setI9LNCD().move(LDAZD.LANC);
		CRRTVIDSDS.setI9ITNO().move(ITMAS.getITNO());
		rRTVIDSpreCall();
		apCall("CRRTVIDS", rRTVIDS);
		rRTVIDSpostCall();
		if (!CRRTVIDSDS.getI9ITDS().isBlank()) {
			ITMAS.setITDS().move(CRRTVIDSDS.getI9ITDS());
		}
	}

	/**
	 *    RETED  - Read CSYTAB  (Terms of delivery)
	 */
	public int baseForCorrectedInvoice() { 	
		//   Read terms of delivery 	
		SYTAB.setCONO(LDAZD.CONO); 	
		SYTAB.setDIVI().clear(); 	
		SYTAB.setSTCO().moveLeftPad("TEDL"); 	
		SYTAB.setSTKY().moveLeftPad(HDISH.getTEDL()); 	
		SYTAB.setLNCD().move(LDAZD.LANC); 	
		if(SYTAB.CHAIN("00", SYTAB.getKey("00"))) { 	
			DSTEDL.setDSTEDL().moveLeft(SYTAB.getPARM());       	
		} else { 	
			DSTEDL.setDSTEDL().clear(); 	
		}        	
		return DSTEDL.getYIDELT(); 	
	}   	

	/**
	 *  Select sublots to process
	 */
	public void assignSublots() {
		XXSBBN.moveLeftPad(this.getBJNO());
		TRSUB.setCONO(HDISH.getCONO());
		TRSUB.setWHLO().move(ITTRA.getWHLO());
		TRSUB.setITNO().move(ITTRA.getITNO());
		TRSUB.setCRDT(ITTRA.getRGDT());
		TRSUB.setCRTE(ITTRA.getRGTM());
		TRSUB.setTMSX(ITTRA.getTMSX());
		TRSUB.setBANO().move(ITTRA.getBANO());
		TRSUB.SETLL("00", TRSUB.getKey("00", 7));
		while (TRSUB.READE("00", TRSUB.getKey("00", 7))) {
			ITSUB.setCONO(TRSUB.getCONO());
			ITSUB.setITNO().move(TRSUB.getITNO());
			ITSUB.setBANO().move(TRSUB.getBANO());
			ITSUB.setBANS(TRSUB.getBANS());
			if (ITSUB.CHAIN_LOCK("00", ITSUB.getKey("00"))) {
				selectedState.moveLeftPad(ITSUB.getSSTS());
				ITSUB.setSSTS().move(cRefSSTSext.IN_PROCESS());
				ITSUB.setSBBN().move(XXSBBN);
				ITSUB.UPDAT("00");
			}
		}
		MMMNGROSDS.setM8SBBN().moveLeftPad(XXSBBN);
	}

	/**
	 *  Reverse sublots
	 */
	public void reverseSublots() {
		ITSUB.setCONO(HDISH.getCONO());
		ITSUB.setSBBN().move(XXSBBN);              
		ITSUB.SETLL("20", ITSUB.getKey("20", 2));
		while (ITSUB.READE("20", ITSUB.getKey("20", 2))) {
			if (ITSUB.getSSTS().EQ("10")) {
				ITSUB.CHAIN_LOCK("00", ITSUB.getKey("00"));
				ITSUB.setSBBN().clear();
				ITSUB.setSSTS().moveLeftPad(selectedState);
				ITSUB.UPDAT("00");
				ITSUB.setSBBN().moveLeftPad(XXSBBN);
			}
		}
	}

	/**
	 *    RNUMO - Convert numeric output
	 */
	public void RNUMO() {
		//   Convert numeric to correct format
		this.PXEDTC = 'P';
		this.PXDCFM = '.';
		this.PXALPH.clear();
		SRCOMNUM.COMNUM();
		XC = check(' ', this.PXALPH) + 1;
		this.PXALPH.setSubstringPad(this.PXALPH, XC - 1);
	}
	/**
	 * RNUMI- Convert Alpha to Numeric
	 * @param field Field name for positioning
	 */
	public void RNUMI(String field) {
		if (this.PXEDTC == ' ') {
			this.PXEDTC = 'P';
		}
		this.PXDCFM = '.';
		this.PXNUM = 0d;
		SRCOMNUM.COMNUM();
		if (SRCOMNUM.PXNMER != 0) {
			IN60 = true;
			// MSGID=XNU0000 Numeric error
			alpha7.move("XNU0000");
			alpha7.moveRight(SRCOMNUM.PXNMER, 1);
			MICommon.setError(field, "alpha7");
		}
		this.PXDCCD = 0;
		this.PXEDTC = 'P';
	}

	/**
	 *    SETLR - End of program
	 */
	public void SETLR() {
		INLR = true;
		super.SETLR(INLR);
	}

	/**
	 *    INIT - Init subroutine
	 */
	public void INIT() {
		MICommon.initiate();
		XXRCD = 0;
	}

	// Movex MDB definitions
	public mvx.db.dta.MHDISH HDISH;
	public mvx.db.dta.MHDISL HDISL;
	public mvx.db.dta.MITMAS ITMAS;
	public mvx.db.dta.MITAUN ITAUN;
	public mvx.db.dta.MITBAL ITBAL;
	public mvx.db.dta.MITLOC ITLOC;
	public mvx.db.dta.MITTRA ITTRA;
	public mvx.db.dta.MMM428 MM428;
	public mvx.db.dta.MHPICH HPICH;
	public mvx.db.dta.CSYTAB SYTAB;
	public mvx.db.dta.MTRSUB TRSUB;
	public mvx.db.dta.MITSUB ITSUB;
	public mvx.db.dta.MGLINE GLINE;
	public mvx.db.dta.ACUORL CUORL;
	public mvx.db.dta.ACUORH CUORH;
	public mvx.db.dta.MITALO ITALO;
	public mvx.db.dta.OODOCU ODOCU;
	public mvx.db.dta.ODHEAD DHEAD;
	public mvx.db.dta.ODEDOC DEDOC; 
	public mvx.db.dta.ODDOCU DDOCU;
	public mvx.db.dta.OOI950 OI950;
	public mvx.db.dta.OOTYPE OTYPE;
	public mvx.db.dta.OOHEAD OHEAD;
	public mvx.db.dta.CMNDIV MNDIV;
	public mvx.db.dta.CMNCMP MNCMP;
	public mvx.db.dta.MGHEAD GHEAD;  
	public mvx.db.dta.MGTYPE GTYPE; 		//A	XPRD0000509 170922
	// Movex MDB definitions end
	public void initMDB() {
		HDISH = (mvx.db.dta.MHDISH)getMDB("MHDISH", HDISH);
		HDISH.setAccessProfile("00", 'R');
		HDISL = (mvx.db.dta.MHDISL)getMDB("MHDISL", HDISL);
		HDISL.setAccessProfile("00", 'R');
		ITMAS = (mvx.db.dta.MITMAS)getMDB("MITMAS", ITMAS);
		ITMAS.setAccessProfile("00", 'R');
		ITAUN = (mvx.db.dta.MITAUN)getMDB("MITAUN", ITAUN);
		ITAUN.setAccessProfile("00", 'R');
		ITBAL = (mvx.db.dta.MITBAL)getMDB("MITBAL", ITBAL);
		ITBAL.setAccessProfile("00", 'R');
		ITLOC = (mvx.db.dta.MITLOC)getMDB("MITLOC", ITLOC);
		ITLOC.setAccessProfile("00", 'R');
		ITTRA = (mvx.db.dta.MITTRA)getMDB("MITTRA", ITTRA);
		ITTRA.setAccessProfile("00", 'R');
		MM428 = (mvx.db.dta.MMM428)getMDB("MMM428", MM428);
		MM428.setAccessProfile("00", 'R');
		HPICH = (mvx.db.dta.MHPICH)getMDB("MHPICH", HPICH);
		HPICH.setAccessProfile("00", 'R');
		SYTAB = (mvx.db.dta.CSYTAB)getMDB("CSYTAB", SYTAB);
		SYTAB.setAccessProfile("00", 'R');
		TRSUB = (mvx.db.dta.MTRSUB)getMDB("MTRSUB", TRSUB);
		TRSUB.setAccessProfile("00", 'R');
		ITSUB = (mvx.db.dta.MITSUB)getMDB("MITSUB", ITSUB);
		ITSUB.setAccessProfile("00", 'R');
		GLINE = (mvx.db.dta.MGLINE)getMDB("MGLINE", GLINE);
		GLINE.setAccessProfile("00", 'R');
		CUORL = (mvx.db.dta.ACUORL)getMDB("ACUORL", CUORL);
		CUORL.setAccessProfile("00", 'R');
		CUORH = (mvx.db.dta.ACUORH)getMDB("ACUORH", CUORH);
		CUORH.setAccessProfile("00", 'R');
		ITALO = (mvx.db.dta.MITALO)getMDB("MITALO", ITALO);
		ITALO.setAccessProfile("00", 'R');
		ODOCU = (mvx.db.dta.OODOCU)getMDB("OODOCU", ODOCU); 	
		ODOCU.setAccessProfile("00", 'R'); 	
		DHEAD = (mvx.db.dta.ODHEAD)getMDB("ODHEAD", DHEAD); 	
		DHEAD.setAccessProfile("00", 'R');
		DEDOC = (mvx.db.dta.ODEDOC)getMDB("ODEDOC", DEDOC); 	
		DEDOC.setAccessProfile("00", 'R');
		DDOCU = (mvx.db.dta.ODDOCU)getMDB("ODDOCU", DDOCU); 	
		DDOCU.setAccessProfile("00", 'U');   
		OI950 = (mvx.db.dta.OOI950)getMDB("OOI950", OI950); 	
		OI950.setAccessProfile("00", 'R');
		OTYPE = (mvx.db.dta.OOTYPE)getMDB("OOTYPE", OTYPE); 	
		OTYPE.setAccessProfile("00", 'R');
		OHEAD = (mvx.db.dta.OOHEAD)getMDB("OOHEAD", OHEAD); 	
		OHEAD.setAccessProfile("00", 'R');
		MNDIV = (mvx.db.dta.CMNDIV)getMDB("CMNDIV", MNDIV);
		MNDIV.setAccessProfile("00", 'R');
		MNCMP = (mvx.db.dta.CMNCMP)getMDB("CMNCMP", MNCMP);
		MNCMP.setAccessProfile("00", 'R');
		GHEAD = (mvx.db.dta.MGHEAD)getMDB("MGHEAD", GHEAD);   
		GHEAD.setAccessProfile("00", 'R');
		GTYPE = (mvx.db.dta.MGTYPE)getMDB("MGTYPE", GTYPE);
	}

	public cMICommon MICommon = new cMICommon(this);
	public boolean XXKIT;
	//*STRUCDEF rMSGSND{
	public MvxStruct rMSGSND = new MvxStruct(800);
	public MvxString MSGSND = rMSGSND.newString(0, 800);
	public MvxString MSGSTS = rMSGSND.newString(0, 13);
	public MvxString MSGCDE = rMSGSND.newString(13, 2);
	public MvxString MSGMSG = rMSGSND.newString(15, 413);
	public MvxString MSGOID = rMSGSND.newString(256, 8);
	public MvxString MSGFLD = rMSGSND.newString(264, 10);
	public int XXIFCA;//*LIKE XXN10
	public int I2;
	public int I1;
	public int XFDOCD;//*LIKE DOCD 	
	public int XFDOC3;//*LIKE DOCD 	
	public int XFDOC2;//*LIKE DOCD 	
	public int XFDOC4;//*LIKE DOCD 	
	public MvxString[] RID = newMvxStrings(999, 10);
	public sMMS428DS MMS428DS = new sMMS428DS(this); 
	public sCRS721DS CRS721DS = new sCRS721DS(this);
	public boolean updateOOI950;
	public int XXRPDT;//*LIKE TRDT
	public sSTMNGLIDDS STMNGLIDDS = new sSTMNGLIDDS(this);
	//*PARAM rMNGLID
	public MvxRecord rMNGLID = new MvxRecord();// len = 559
	public void rMNGLIDpreCall() {// insert param into record for call
		rMNGLID.reset();
		rMNGLID.set(APIDS);
		rMNGLID.set(STMNGLIDDS.getSTMNGLIDDS());
	}
	public void rMNGLIDpostCall() {// extract param from record after call
		rMNGLID.reset();
		rMNGLID.getString(APIDS);
		rMNGLID.getString(STMNGLIDDS.setSTMNGLIDDS());
	}
	public MvxStruct rDSWHSL = new MvxStruct(10);
	public MvxString DSWHSL = rDSWHSL.newString(0, 10);
	public MvxString DSWHS1 = rDSWHSL.newString(0, 3);
	public MvxString DSSLTP = rDSWHSL.newString(3, 2);
	public MvxString DSWHS2 = rDSWHSL.newString(5, 3);
	public sMMMNGRORDS MMMNGRORDS = new sMMMNGRORDS(this);  
	public MvxString XXSBBN = cRefSBBN.likeDef();
	public MvxString selectedState = cRefSSTS.likeDef();
	public cRetrieveUserInfo retrieveUserInfo = new cRetrieveUserInfo(this);
	//*PARAM rRTVIDS{
	public MvxRecord rRTVIDS = new MvxRecord();// len = 158
	public void rRTVIDSpreCall() {// insert param into record for call
		rRTVIDS.reset();
		rRTVIDS.set(CRRTVIDSDS.getCRRTVIDSDS());
	}
	public void rRTVIDSpostCall() {// extract param from record after call
		rRTVIDS.reset();
		rRTVIDS.getString(CRRTVIDSDS.setCRRTVIDSDS());
	}
	public sCRRTVIDSDS CRRTVIDSDS = new sCRRTVIDSDS(this);
	public sDSTEDL DSTEDL = new sDSTEDL(this); 	
	public sMMMNGROSDS MMMNGROSDS = new sMMMNGROSDS(this);
	public MvxRecord rMNGROS = new MvxRecord();
	public void rMNGROSpreCall() {// insert param into record for call
		rMNGROS.reset();
		rMNGROS.set(APIDS);
		rMNGROS.set(MMMNGROSDS.getMMMNGROSDS());
	}
	public void rMNGROSpostCall() {// extract param from record after call
		rMNGROS.reset();
		rMNGROS.getString(APIDS);
		rMNGROS.getString(MMMNGROSDS.setMMMNGROSDS());
	}
	//*STRUCDEF rAPIDS{
	public MvxStruct rAPIDS = new MvxStruct(413);
	public MvxString APIDS = rAPIDS.newString(0, 413);
	public MvxString PXENV = rAPIDS.newChar(0);
	public MvxString PXOPC = rAPIDS.newString(1, 10);
	public MvxString PXIN60 = rAPIDS.newChar(11);
	public MvxString PXMSID = rAPIDS.newString(12, 7);
	public MvxString PXMSGD = rAPIDS.newString(19, 256);
	public MvxString PXMSG = rAPIDS.newString(275, 128);
	public MvxString PXCHID = rAPIDS.newString(403, 10);
	public MvxString Z0PICC = new MvxString(2);//*LIKE XXA2
	public MvxString XXWHLO = new MvxString(3);//*LIKE XXA2
	public MvxString XLWHSL = new MvxString(10);//*LIKE WHSL
	public MvxString XLWHLO = cRefWHLO.likeDef();
	public double XBCAWE;
	public int XXRCD;
	public int XXCONO;
	public char CHREC = ' ';
	public int XC;
	public double XXTRQT;
	public cSRIMPI SRIMPI = new cSRIMPI(this);
	public cPLCHKAD PLCHKAD = new cPLCHKAD(this);
	public cSREMPI SREMPI = new cSREMPI(this);
	public MvxString XXDIVI = new MvxString(3);
	public MvxString XXKEY1 = new MvxString(14);
	public MvxString XXINTN = new MvxString(15);
	public MvxString alpha7 = new MvxString(7);
	public MvxString alpha3 = new MvxString(3);
	public MvxString XXLINE = new MvxString(7);
	public sMMS100DS MMS100DS = new sMMS100DS(this); 		//A	XPRD0000509 170922
	public sATMNGATRDS ATMNGATRDS = new sATMNGATRDS(this); 		//A	XPRD0000509 170922
	public MvxString[] BEX = newMvxStrings(299, 10);//*LIKE XXA10 		//A	XPRD0000509 170922
	public int FL;//*LIKE XXN30 		//A	XPRD0000509 170922
	public MvxString[] BFD = {// Panel D
			new MvxString(16, "BXTRNR    005018"), new MvxString(16, "/END/           "), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""),
			new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, ""), new MvxString(16, "")};//*LIKE XXA16

	//*STRUCDEF rMSGREC{
	public MvxStruct rMSGREC = new MvxStruct(512);
	public MvxString MSGREC = rMSGREC.newString(0, 512);
	public MvxString MSGCMD = rMSGREC.newString(0, 15);
	public MvxString MSGDAT = rMSGREC.newString(15, 413);
	public MvxString MSSEND = rMSGREC.newString(0, 3);
	public MvxString Q1DTFM = rMSGREC.newString(15, 4);
	public MvxString Q1DSEP = rMSGREC.newChar(19);
	public MvxString XXWHSL = new MvxString(10);
	//*PARAM rMNGROR{
	public MvxRecord rMNGROR = new MvxRecord();// len = 506

	public void rMNGRORpreCall() {// insert param into record for call
		rMNGROR.reset();
		rMNGROR.set(APIDS);
		rMNGROR.set(MMMNGRORDS.getMMMNGRORDS());
	}

	public void rMNGRORpostCall() {// extract param from record after call

		rMNGROR.reset();
		rMNGROR.getString(APIDS);
		rMNGROR.getString(PXQ0DS);
		rMNGROR.unGet();
		rMNGROR.getString(MMMNGRORDS.setMMMNGRORDS());
	}

	//*PARAM rMMS100{
	public MvxRecord rMMS100 = new MvxRecord();// len = 3904

	public void rMMS100preCall() {// insert param into record for call
		rMMS100.reset();
		rMMS100.set(MMS100DS.getMMS100DS());
		rMMS100.set(BEX);
		rMMS100.set(BXPGNM);
	}

	public void rMMS100postCall() {// extract param from record after call

		rMMS100.reset();
		rMMS100.getString(MMS100DS.setMMS100DS());
		rMMS100.getStringArray(BEX);
		rMMS100.getString(BXPGNM);
	}
	public MvxRecord rMNGATR = new MvxRecord();// len = 617

	public void rMNGATRpreCall() {// insert param into record for call
		rMNGATR.reset();
		rMNGATR.set(APIDS);
		rMNGATR.set(ATMNGATRDS.getATMNGATRDS());
	}

	public void rMNGATRpostCall() {// extract param from record after call
		rMNGATR.reset();
		rMNGATR.getString(APIDS);
		rMNGATR.getString(ATMNGATRDS.setATMNGATRDS());
	}
	public MvxString PXQ0DS = new MvxString(93);//*LIKE XXA93
	public MvxString BXPGNM = new MvxString(10);//*LIKE PGNM 		//A	XPRD0000509 170922
	public void rPWFLDSyncFrom() {
		moveArray(SREMPI.FLD, SRIMPI.FLD);
	}

	public String getVarList(java.util.Vector v) {
		super.getVarList(v);
		v.addElement(HDISH);
		v.addElement(GHEAD);
		v.addElement(HDISL);
		v.addElement(ITMAS);
		v.addElement(ITAUN);
		v.addElement(ITBAL);
		v.addElement(ITLOC);
		v.addElement(ITTRA);
		v.addElement(MM428);
		v.addElement(HPICH);
		v.addElement(SYTAB);
		v.addElement(TRSUB);
		v.addElement(ITSUB);
		v.addElement(MICommon);
		v.addElement(retrieveUserInfo);
		v.addElement(SRIMPI);
		v.addElement(PLCHKAD);
		v.addElement(SREMPI);
		v.addElement(XXDIVI);
		v.addElement(XXKEY1);
		v.addElement(XXINTN);
		v.addElement(alpha7);
		v.addElement(alpha3);
		v.addElement(XXLINE);
		v.addElement(XXWHLO);
		v.addElement(MMMNGRORDS);
		v.addElement(rMSGSND); 		//A	XPRD0000508 170919S
		v.addElement(rMSGREC); 		//A	XPRD0000508 170919S
		return version;
	}

	public void clearInstance() {
		super.clearInstance();
		XXRCD = 0;
		XXCONO = 0;
		CHREC = ' ';
		XC = 0;
	}

	//*STRUCDEF rXXDTN{
	public MvxStruct rXXDTN = new MvxStruct(15);
	public MvxString XXDTN = rXXDTN.newDouble(0, 15, 6);
	public MvxString XXDTA = rXXDTN.newString(0, 15);

	public String getVer() {
		return version;
	}

	public final String version = "Pgm.Name: CRCU20MI, " + "Source creation date: Tue Jan 15 14:24:55 CET 2002, " + "ID number: 1011101095235";

	public String getVersion() {
		return _version;
	}

	public String getRelease() {
		return _release;
	}

	public String getSpLevel() {
		return _spLevel;
	}

	public String getSpNumber() {
		return _spNumber;
	}

	public final static String _version = "15";
	public final static String _release = "1";
	public final static String _spLevel = "0";
	public final static String _spNumber ="MAK_SANDESH.PAWAR_210113_08:58";
	public final static String _GUID = "97942E576D9340e8843A901D1E0025A2";
	public final static String _tempFixComment = "";
	public final static String _build = "000000000000025";
	public final static String _pgmName = "CRCU20MI";

	public String getGUID() {
		return _GUID;
	}

	public String getTempFixComment() {
		return _tempFixComment;
	}

	public String getVersionInformation() {
		return _version + '.' + _release + '.' + _spLevel + ':' + _spNumber;
	}

	public String getBuild() {
		return (_version + _release + _build + "      " + _pgmName + "                                   ").substring(0, 34);
	}

	/**
	 *    MDFDS - Move data from DSPF-fields to DS-fields
	 */
	public void MDFDS() {
		MMS100DS.setBXCHID().move(this.DSUSS);
		MMS100DS.setBPDTFM().move(MNDIV.getDTFM());//overridden to WWDTFM for P panel
		MMS100DS.setBCTRNR().clear(); 	
		MMS100DS.setBXFACI().move(GHEAD.getFACI());
		MMS100DS.setBXMODL().move(GHEAD.getMODL());
		MMS100DS.setBXTEDL().move(GHEAD.getTEDL());
		MMS100DS.setBXTRTP().move(GHEAD.getTRTP());
		MMS100DS.setBXTRNR().move(GHEAD.getTRNR());
		MMS100DS.setBXATHS(GHEAD.getATHS());
		MMS100DS.setBXTRSL().move(GHEAD.getTRSL());
		MMS100DS.setBXTRSH().move(GHEAD.getTRSH());
		MMS100DS.setBXDSP3(1);
		MMS100DS.setBXCONO(GHEAD.getCONO());
		//----------------------------------------------------------- 	 	
		//----------------------------------------------------------- 	 	
		//MMS100DS.setBXRORC(DSP.WGRORC); 		//D	XPRD0000509 170922
		//MMS100DS.setBXRORN().move(DSP.WGRORN); 		//D	XPRD0000509 170922
		//MMS100DS.setBXRORL(DSP.WGRORL); 		//D	XPRD0000509 170922
		//MMS100DS.setBXRORX(DSP.WGRORX); 		//D	XPRD0000509 170922
		//MMS100DS.setBXWHLO().move(DSP.WGWHLO); 		//D	XPRD0000509 170922
		MMS100DS.setBXMWHE(GHEAD.getMWHE());
		//MMS100DS.setBXTWSL().move(DSP.WGTWSL); 		//D	XPRD0000509 170922
		//MMS100DS.setBXPRIO(DSP.WGPRIO); 		//D	XPRD0000509 170922
		//MMS100DS.setBXIRCV().move(DSP.WGIRCV); 		//D	XPRD0000509 170922
		//MMS100DS.setBXGSR3().move(DSP.WGGSR3); 		//D	XPRD0000509 170922
		//MMS100DS.setBXREMK().move(DSP.WGREMK); 		//D	XPRD0000509 170922
		//MMS100DS.setBXGSR1().move(DSP.WGGSR1); 		//D	XPRD0000509 170922
		//MMS100DS.setBXGSR2().move(DSP.WGGSR2); 		//D	XPRD0000509 170922
		MMS100DS.setBXNUGL(GHEAD.getNUGL());
		//MMS100DS.setBXDEPT().move(DSP.WGDEPT); 		//D	XPRD0000509 170922
		//MMS100DS.setBXTOFP().move(DSP.WGTOFP); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOPNO(DSP.WGOPNO); 		//D	XPRD0000509 170922
		//MMS100DS.setBXREVN().move(DSP.WGREVN); 		//D	XPRD0000509 170922
		MMS100DS.setBXGRWE(GHEAD.getGRWE());
		//MMS100DS.setBXPROJ().move(DSP.WGPROJ); 		//D	XPRD0000509 170922
		//MMS100DS.setBXELNO().move(DSP.WGELNO); 		//D	XPRD0000509 170922
		//MMS100DS.setBXNTAM().move(DSP.WGNTAM); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIN1(DSP.WBOIN1); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIN1(DSP.WGOIN2); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIN1(DSP.WGOIN3); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIN1(DSP.WGOIN4); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIN1(DSP.WGOIN5); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIN1(DSP.WGOIN6); 		//D	XPRD0000509 170922
		//MMS100DS.setBXRGDT().move(DSP.WWRGDT); 		//D	XPRD0000509 170922
		//MMS100DS.setBXLMDT().move(DSP.WWLMDT); 		//D	XPRD0000509 170922
		//} else { 		//D	XPRD0000509 170922
		//if (picGetPanel() == 'F' || 		//D	XPRD0000509 170922
		//X0PICX == 'F' && 		//D	XPRD0000509 170922
		//picGetPanel() != 'E' && 		//D	XPRD0000509 170922
		//picGetPanel() != 'F') { 		//D	XPRD0000509 170922
		MMS100DS.setBXFACI().move(GHEAD.getFACI()); 		//D	XPRD0000509 170922
		MMS100DS.setBXMODL().move(GHEAD.getMODL());
		MMS100DS.setBXTEDL().move(GHEAD.getTEDL());
		MMS100DS.setBXTRTP().move(GHEAD.getTRTP());
		MMS100DS.setBXTRNR().move(GHEAD.getTRNR());
		//MMS100DS.setB1TRTP().move(DSP.WHTRTP); 		//D	XPRD0000509 170922
		//MMS100DS.setBXRESP().move(DSP.WGRESP); 		//D	XPRD0000509 170922
		MMS100DS.setBXTRSL().move(GHEAD.getTRSL());
		MMS100DS.setBXTRSH().move(GHEAD.getTRSH());
		//MMS100DS.setBXTRDT().move(DSP.WGTRDT); 		//D	XPRD0000509 170922
		//MMS100DS.setBXTRT1(DSP.WGTRT1); 		//D	XPRD0000509 170922
		//MMS100DS.setBXTRT2(DSP.WGTRT2); 		//D	XPRD0000509 170922
		//MMS100DS.setBXTRT3(WGTRT3.getInt()); 		//D	XPRD0000509 170922
		//MMS100DS.setBXTRDY(DSP.WGTRDY); 		//D	XPRD0000509 170922
		//MMS100DS.setBXTRE1(DSP.WGTRE1); 		//D	XPRD0000509 170922
		//MMS100DS.setBXTRE2(DSP.WGTRE2); 		//D	XPRD0000509 170922
		//MMS100DS.setBXRIDT().move(DSP.WGRIDT); 		//D	XPRD0000509 170922
		//MMS100DS.setBXRIT1(DSP.WGRIT1); 		//D	XPRD0000509 170922
		//MMS100DS.setBXRIT2(DSP.WGRIT2); 		//D	XPRD0000509 170922
		//if (MMS100DS.getBXWHLO().NE(DSP.WGWHLO)) { 		//D	XPRD0000509 170922
		//MMS100DS.setBXWHLO().move(DSP.WGWHLO); 		//D	XPRD0000509 170922
		//RDPO(); 		//D	XPRD0000509 170922
		//DSP.WFSCED = HDIPO.getSCED(); 		//D	XPRD0000509 170922
		//} else { 		//D	XPRD0000509 170922
		//MMS100DS.setBXWHLO().move(DSP.WGWHLO); 		//D	XPRD0000509 170922
		//} 		//D	XPRD0000509 170922
		//MMS100DS.setBXTWLO().move(DSP.WGTWLO); 		//D	XPRD0000509 170922
		//MMS100DS.setBXPRIO(DSP.WGPRIO); 		//D	XPRD0000509 170922
		//MMS100DS.setBXTWSL().move(DSP.WGTWSL); 		//D	XPRD0000509 170922
		//MMS100DS.setBXGSR3().move(DSP.WGGSR3); 		//D	XPRD0000509 170922
		//MMS100DS.setBXREMK().move(DSP.WGREMK); 		//D	XPRD0000509 170922
		MMS100DS.setBXNUGL(GHEAD.getNUGL());
		MMS100DS.setBXHAZI(GHEAD.getHAZI());
		//MMS100DS.setBXTOFP().move(DSP.WGTOFP); 		//D	XPRD0000509 170922
		//MMS100DS.setBXAURE().move(DSP.WGAURE); 		//D	XPRD0000509 170922
		MMS100DS.setBXATHS(GHEAD.getATHS());
		//MMS100DS.setBXAUTD().move(DSP.WWAUTD); 		//D	XPRD0000509 170922
		MMS100DS.setBXGRWE(GHEAD.getGRWE());
		//MMS100DS.setBXRORC(DSP.WGRORC); 		//D	XPRD0000509 170922
		//MMS100DS.setBXRORN().move(DSP.WGRORN); 		//D	XPRD0000509 170922
		//MMS100DS.setBXRORL(DSP.WGRORL); 		//D	XPRD0000509 170922
		//MMS100DS.setBXRORX(DSP.WGRORX); 		//D	XPRD0000509 170922
		//MMS100DS.setBXNTAM().move(DSP.WGNTAM); 		//D	XPRD0000509 170922
		//MMS100DS.setBXPROJ().move(DSP.WGPROJ); 		//D	XPRD0000509 170922
		//MMS100DS.setBXELNO().move(DSP.WGELNO); 		//D	XPRD0000509 170922
		//MMS100DS.setBXSCED(DSP.WFSCED); 		//D	XPRD0000509 170922
		////-------------------------------------------------------- 		//D	XPRD0000509 170922
		//if (DSP.WFSCED != XXSCED) { 		//D	XPRD0000509 170922
		//MMS100DS.setBXDSP7(1); 		//D	XPRD0000509 170922
		//} 		//D	XPRD0000509 170922
		////-------------------------------------------------------- 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIN1(DSP.WGOIN1); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIN1(DSP.WGOIN2); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIN1(DSP.WGOIN3); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIN1(DSP.WGOIN4); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIN1(DSP.WGOIN5); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIN1(DSP.WGOIN6); 		//D	XPRD0000509 170922
		//MMS100DS.setBXRGDT().move(DSP.WWRGDT); 		//D	XPRD0000509 170922
		//MMS100DS.setBXLMDT().move(DSP.WWLMDT); 		//D	XPRD0000509 170922

		MMS100DS.setBXTRNR().move(GHEAD.getTRNR());
		MMS100DS.setBXIN70(toChar(IN70));
		//MMS100DS.setBXWHIN().move(CRS788DS.getYMWHIN()); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOINV().moveLeftPad(XXOINV); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIDN().moveLeftPad(XXOIDN); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIDL(XXOIDL); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIDX(XXOIDX); 		//D	XPRD0000509 170922
		//MMS100DS.setBXOIDO(XXOIDI); 		//D	XPRD0000509 170922
	}

   public String [][] getCustomerModification() {
      return _customerModifications;
   } // end of method [][] getCustomerModification()

   public final static String [][] _customerModifications={
      {"JNS012","170317","SOHCHA","DO Reversal"},
      {"JNS01200","170321","WILDSO","Rename DO Reversal to Reverse Pick"},
      {"JNS01201","170321","WILDSO","Mandatory Checks not handled"},
      {"JNS01202","170327","SOHCHA","Pick reversal for multiple lines"},
      {"JNS01203","170329","JYOSHI","Create revese Pick reciept"},
      {"JNS01204","170407","JYOSHI","Correction to reverse picklist"},
      {"XPRD00004","170802","WILDSO","Programs PRD movement"},
      {"XPRD0000508","170920","JYOSHI","CRCU20MI not throwing error messages"},
      {"XPRD0000509","170925","JYOSHI","CRCU20MI is not error messages"},
      {"XPRD0000511","171003","JYOSHI","Do not delete if the order status is 99 and if DO line exists"}
   };
}