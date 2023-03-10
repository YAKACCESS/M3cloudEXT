/****************************************************************************************
 Extension Name: AbbreviateUtils
 Type : ExtendM3Utility
 Script Author: Billy Willoughby
 Date: 2022-05-01
 Description:
  EXT001 is a Batch Job for processing Geocode requests for Sovos
  in M3.  Uses Sovos API via IONAPI to connect to WBI function.
  The job is triggered via an M3 trigger in CMS045.
    
 Revision History:
 Name                    Date             Version          Description of Changes
 Billy Willoughby        2022-05-18       1.0              Adding Header
 ******************************************************************************************/


class AbbreviateUtils extends ExtendM3Utility {

  /**
   * AbbreviateName - A function to consistently shorted common phrases
   * @param input - String to shorten
   * @return - Shortened String
   */
  String AbbreviateName (String input) {

    input = input.replace('United States', 'US')
    input = input.replace('Federal', 'FED')
    input = input.replace('Sales Tax', 'Tax')
    input = input.replace('State Tax', '')
    input = input.replace('Export', 'EXP')
    input = input.replace(':', '')
    return input
  }  
  
}
