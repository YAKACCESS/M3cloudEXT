/****************************************************************************************
 Extension Name: CrtReversePick
 Type : ExtendM3Transaction
 Script Author: Billy Willoughby
 Date: 2023-03-01
 Description:
    * MWS442 - Reverse Receipt as an MI

 Revision History:
 Name                    Date             Version          Description of Changes
 Billy Willoughby        2023/03/01       1.0              Initial Version
 ******************************************************************************************/

class CrtReversePick extends ExtendM3Transaction {

  private final MIAPI mi
  private final DatabaseAPI database
  private final LoggerAPI logger
  private final UtilityAPI utility 
  private final MICallerAPI miCaller
  private final ProgramAPI program

  /**
   * Infor Constuctor for this XTendM3 API
   * @param databaseAPI Infor Database API Interface
   * @param loggerAPI Infor Logger API Inteface
   * @param mi Infor Interface for this XTendM3 API
   */
  CrtReversePick(UtilityAPI utilityAPI, DatabaseAPI databaseAPI, LoggerAPI loggerAPI, MIAPI mi, MICallerAPI miCaller, ProgramAPI program) {
    this.mi = mi
    this.database = databaseAPI
    this.logger = loggerAPI
    this.utility = utilityAPI
    this.miCaller = miCaller
    this.program = program
  }

  /**
   * MI Entry Point
   * Sets up MI Parameters and Initial Database Read
   */
  void main() {
    /*All Input parameters are mandatory*/
  
    String cono = (mi.inData.get('CONO') == null) ? '' : (String) mi.inData.get('CONO').trim() 
    String dlix = (mi.inData.get('DLIX') == null) ? '' : (String) mi.inData.get('DLIX').trim() 
    String plsx = (mi.inData.get('PLSX') == null) ? '' : (String) mi.inData.get('PLSX').trim() 
    String whlo = (mi.inData.get('WHLO') == null) ? '' : (String) mi.inData.get('WHLO').trim() 

    logger.debug("CrtReversePick Request: ${cono}, ${divi}, ${plsx}, ${whlo}")

    if (validateCono(cono)  && validateDlix(cono, dlix)) {
      processReversePick(cono, dlix, plsx, whlo)
    }
    else {         
      String oErrorText = "Validation failed for ${cono}/${dlix}, verify values."
      mi.error(oErrorText)
      return
    }

    mi.outData.put('DLIX', dlix)
    mi.outData.put('MESG', '')
    mi.write()

  /*
  MMS100 - MGHEAD - TRNR
  MWS442 - DLIX 
    RORC = 5
    TYPE 51 DO Issue


  */

  }

  private void  processReversePick(String cono, String dlix, String plsx, String whlo) {
  //if RORC != 4 or 5 then set RIDN => TRNR
  /*MHDISH90
  OQCONO	Company	Asc
  OQINOU	Direction	Asc
  OQWHLO	Warehouse	Asc
  OQDLIX	Delivery number	Asc*/

    DBAction query = database.table('MHDISH').index('90').selection('OQCONO', 'OQINOU', 'OQWHLO', 'OQDLIX').build()
 
    DBContainer container = query.getContainer()
    container.set ('OQCONO', Integer.parseInt(cono))
    container.set ('OQINOU', 1)
    container.set ('OQWHLO', Integer.parseInt(plsx))
    container.set ('OQDLIX', whlo)

    if (!query.read(container)) {
      container.set ('OQINOU', 2)}
    
    if (!query.read(container)) {
      String oErrorText = "Delivery does not exist ${cono}/${dlix}"
      mi.error(oErrorText)
      return
    }

  }

 /**
   * Validate Company Number
   * @param cono - M3 Company Number
   * @return bool, return true if valid
   */
  private boolean validateCono(Integer cono) {
    logger.debug("Validating CONO: ${cono}")
    boolean valid = false
    Map <String, String> params = ['CONO': cono.toString()]

    this.miCaller.call('MNS095MI', 'Get', params, {
      Map <String, String> response ->
      if (response != null && response.get('CONO') != null && response.get('CONO').toString().trim() == cono.toString().trim()) {
        logger.debug("Company Validated: ${cono}")
        this.setJobStatus(20, "Company Validated: ${cono}")
        valid = true
      }
      else {
        logger.debug("Request ${params}, Response: ${response}")
        this.setJobStatus(15, "Company ${cono} Invalid, job aborted ")    
      }

    })

    return valid
  }

  
  /**
   * Validate Delivery Number
   * @param cono - M3 Company 
   * @param dlix - M3 Delivery Number, returns valid on 0
   * @return - bool - returns if valid 
   */
  private boolean validateDlix(Integer cono, String dlix) {
    logger.debug("Validating DLIX: ${dlix}")
    this.setJobStatus(20, "Checking Delivery: ${dlix}")
    boolean valid = false
    Map <String, String> params = ['cono':cono.toString(), 'DLIX': dlix]

    this.miCaller.call('MWS410MI', 'GetHead', params, {
      Map <String, String> response ->
      if (response != null && response.get('DLIX') != null && response.get('DLIX').toString().trim() == dlix.toString().trim()) {
        this.setJobStatus(20, "Delivery Validated: ${dlix}")
        valid = true
      }
      else {
        logger.debug("Request ${params}, Response: ${response}")
        this.setJobStatus(15, "Delivery ${dlix} Invalid, job aborted ")    
      }
    })
    
    return valid
  }

} 
