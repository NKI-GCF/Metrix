// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.constants;

public final class Constants {

	// InterOp file names
	public static final String 		CONTROL_METRICS 			= "ControlMetricsOut.bin";
	public static final String 		CORRECTED_INT_METRICS 		= "CorrectedIntMetricsOut.bin";
	public static final String 		ERROR_METRICS 				= "ErrorMetricsOut.bin";
	public static final String 		EXTRACTION_METRICS 			= "ExtractionMetricsOut.bin";
	public static final String 		IMAGE_METRICS 				= "ImageMetricsOut.bin";
	public static final String 		INDEX_METRICS 				= "IndexMetricsOut.bin";
	public static final String 		QMETRICS_METRICS 			= "QMetricsOut.bin";
	public static final String 		TILE_METRICS 				= "TileMetricsOut.bin";

	// Run states
	public static final int 		STATE_RUNNING				= 1;
	public static final int 		STATE_FINISHED				= 2;
	public static final int 		STATE_HANG					= 3;
	public static final int 		STATE_TURN					= 4;
	public static final int 		STATE_INIT					= 5;
	public static final int			STATE_ALL_PSEUDO			= 12;

	// Metric outcome classification
	public static final int 		METRIC_LOWCD				= 1;
	public static final int 		METRIC_AVGCD				= 2;
	public static final int 		METRIC_HGHCD				= 3;

	public static final int 		METRIC_CD_LOW				= 300000;
	public static final int 		METRIC_CD_AVG				= 600000;
	public static final int 		METRIC_CD_HIGH				= 900000;

	// Constants for the average corrected intensities metrics
	public static final String		METRIC_VAR_ACI				= "AvgCorInt";
	
	public static final String		METRIC_VAR_ACI_A			= "AvgCorIntA";
	public static final String		METRIC_VAR_ACI_C			= "AvgCorIntC";
	public static final String		METRIC_VAR_ACI_G			= "AvgCorIntG";
	public static final String		METRIC_VAR_ACI_T			= "AvgCorIntT";

	public static final String		METRIC_VAR_ACICC_A			= "AvgCorIntClusA";
	public static final String		METRIC_VAR_ACICC_C			= "AvgCorIntClusC";
	public static final String		METRIC_VAR_ACICC_G			= "AvgCorIntClusG";
	public static final String		METRIC_VAR_ACICC_T			= "AvgCorIntClusT";

	public static final String		METRIC_VAR_NUM_BCS_NC		= "BaseCallsNoCall";	// Float
	public static final String		METRIC_VAR_NUM_BCS_A		= "BaseCallsA";			// UnsignedShort (uint16)
	public static final String		METRIC_VAR_NUM_BCS_C		= "BaseCallsC";			// UnsignedShort (uint16)
	public static final String		METRIC_VAR_NUM_BCS_G		= "BaseCallsG";			// UnsignedShort (uint16)
	public static final String		METRIC_VAR_NUM_BCS_T		= "BaseCallsT";			// UnsignedShort (uint16)
	public static final String		METRIC_VAR_NUM_SIGNOISE		= "SigNoiseRatio";		// Float
	
	// InterOp metric codes
	public static final int 		TILE_CLUSTER_DENSITY 		= 100;
	public static final int			TILE_CLUSTER_DENSITY_PF		= 101;
	public static final int			TILE_NUM_CLUSTERS			= 102;
	public static final int			TILE_NUM_CLUSTERS_PF		= 103;


	// Command data request metric specification
	public static final String		METRIC_TYPE_QSCORE			= "QSCORE";
	public static final String 		METRIC_TYPE_CD				= "CLUSTER_DENSITY";
	public static final String 		METRIC_TYPE_PHASING			= "PHASING";
	public static final String		METRIC_TYPE_PREPHASING		= "PREPHASING";

	public static final String[]	METRIC_TYPE_REQUEST			= {
															METRIC_TYPE_QSCORE,
															METRIC_TYPE_CD,
															METRIC_TYPE_PHASING,
															METRIC_TYPE_PREPHASING
															};
	
	// Command function types
	public static final String 		COM_FUNCTION_SET			= "SET";
	public static final String		COM_FUNCTION_FETCH			= "FETCH";

	// Command data request formats
	public static final	String		COM_FORMAT_XML				= "XML";
	public static final	String		COM_FORMAT_OBJ				= "POJO";
	public static final String		COM_FORMAT_TAB				= "TAB";

	// Command data request detail level
	public static final	String		COM_TYPE_SIMPLE				= "SIMPLE";
	public static final	String		COM_TYPE_METRIC				= "METRIC";
	public static final	String		COM_TYPE_DETAIL				= "DETAIL";
	public static final	String		COM_TYPE_FULL				= "FULL";

	// Command data request modes
	public static final String		COM_MODE_CALL				= "CALL";
	public static final String		COM_MODE_TIMED				= "TIMED";

	public static final String		COM_RET_TYPE_BYSTATE		= "BYSTATE";
	public static final String		COM_RET_TYPE_BYRUN			= "BYRUN";

	public static final long		METRIC_UPDATE_TIME			= 7200000;	// Update every 120 minutes

	private Constants(){
	    //this prevents even the native class from 
	    //calling this ctor as well :
	    throw new AssertionError();
  	}
}
