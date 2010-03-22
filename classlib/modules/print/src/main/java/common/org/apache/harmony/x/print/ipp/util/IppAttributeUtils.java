/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/** 
 * @author Igor A. Pyankov 
 */ 

package org.apache.harmony.x.print.ipp.util;

import javax.print.attribute.Attribute;
import javax.print.attribute.DateTimeSyntax;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.SetOfIntegerSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.TextSyntax;
import javax.print.attribute.URISyntax;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.ColorSupported;
import javax.print.attribute.standard.Compression;
import javax.print.attribute.standard.Fidelity;
import javax.print.attribute.standard.Finishings;
import javax.print.attribute.standard.JobSheets;
import javax.print.attribute.standard.JobState;
import javax.print.attribute.standard.JobStateReason;
import javax.print.attribute.standard.MediaName;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.MultipleDocumentHandling;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PDLOverrideSupported;
import javax.print.attribute.standard.PresentationDirection;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterState;
import javax.print.attribute.standard.PrinterStateReason;
import javax.print.attribute.standard.ReferenceUriSchemesSupported;
import javax.print.attribute.standard.Severity;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;

import org.apache.harmony.x.print.attributes.PPDMediaSizeName;
import org.apache.harmony.x.print.ipp.IppAttribute;

/** 
 * Supporing class for Ipp2Java 
 */

public class IppAttributeUtils {
    public static Object getIppValue(Attribute attr, byte ippvtag) {
        Object o = null;

        switch (ippvtag) {
        // integer values for the "value-tag" field.
        case IppAttribute.TAG_BOOLEAN:
        case IppAttribute.TAG_INTEGER:
        case IppAttribute.TAG_ENUM:
            if (attr instanceof IntegerSyntax) {
                o = new Integer(((IntegerSyntax) attr).getValue());
            } else if (attr instanceof EnumSyntax) {
                o = new Integer(((EnumSyntax) attr).getValue());
            } else if (attr instanceof DateTimeSyntax
                    || attr instanceof ResolutionSyntax
                    || attr instanceof SetOfIntegerSyntax
                    || attr instanceof Size2DSyntax
                    || attr instanceof TextSyntax || attr instanceof URISyntax) {
                // TODO - process other attr's types
            }
            break;
        // octetString values for the "value-tag" field.
        case IppAttribute.TAG_DATETIME:
        case IppAttribute.TAG_RESOLUTION:
        case IppAttribute.TAG_RANGEOFINTEGER:
        case IppAttribute.TAG_OCTETSTRINGUNSPECIFIEDFORMAT:
        case IppAttribute.TAG_TEXTWITHLANGUAGE:
        case IppAttribute.TAG_NAMEWITHLANGUAGE:
            if (attr instanceof IntegerSyntax) {
                // TODO - it seems that this needs to be fixed
                o = new Integer(((IntegerSyntax) attr).toString());
            } else if (attr instanceof EnumSyntax) {
                // TODO - it seems that this needs to be fixed
                o = new Integer(((EnumSyntax) attr).toString());
            } else if (attr instanceof DateTimeSyntax
                    || attr instanceof ResolutionSyntax
                    || attr instanceof SetOfIntegerSyntax
                    || attr instanceof Size2DSyntax) {
                // TODO - process other attr's types
            } else if (attr instanceof TextSyntax) {
                // TODO - it seems that this needs to be fixed
                o = new Integer(((TextSyntax) attr).toString());
            } else if (attr instanceof URISyntax) {
                // TODO - it seems that this needs to be fixed
                o = new Integer(((URISyntax) attr).toString());
            }
            break;
        // character-string values for the "value-tag" field
        case IppAttribute.TAG_TEXTWITHOUTLANGUAGE:
        case IppAttribute.TAG_NAMEWITHOUTLANGUAGE:
        case IppAttribute.TAG_KEYWORD:
        case IppAttribute.TAG_URI:
        case IppAttribute.TAG_URISCHEME:
        case IppAttribute.TAG_CHARSET:
        case IppAttribute.TAG_NATURAL_LANGUAGE:
        case IppAttribute.TAG_MIMEMEDIATYPE:
            if (attr instanceof IntegerSyntax) {
                o = ((IntegerSyntax) attr).toString();
            } else if (attr instanceof EnumSyntax) {
                o = ((EnumSyntax) attr).toString();
            } else if (attr instanceof DateTimeSyntax
                    || attr instanceof ResolutionSyntax
                    || attr instanceof SetOfIntegerSyntax
                    || attr instanceof Size2DSyntax) {
                // TODO - process other attr's types
            } else if (attr instanceof TextSyntax) {
                o = ((TextSyntax) attr).toString();
            } else if (attr instanceof URISyntax) {
                o = ((URISyntax) attr).toString();
            }
            break;
        default:
            break;
        }

        return o;
    }

    public static Object getObject(Class e, int eval) {
        Object o = null;
        EnumSyntax[] et = getEnumValueTable(e);

        if (et != null) {
            for (int i = 0, ii = et.length; i < ii; i++) {
                if (et[i] != null && et[i].getValue() == eval) {
                    o = et[i];
                    break;
                }
            }
        }

        return o;
    }

    public static Object getObject(Class e, String eval) {
        Object o = null;
        EnumSyntax[] et = getEnumValueTable(e);

        if (et != null) {
            for (int i = 0, ii = et.length; i < ii; i++) {
                if (et[i] != null && et[i].toString().equals(eval)) {
                    o = et[i];
                    break;
                }
            }
        }

        return o;
    }

    public static EnumSyntax[] getEnumValueTable(Class e) {
        for (int i = 0, ii = enumTables.length; i < ii; i++) {
            if (enumTables[i][0] == e) {
                return (EnumSyntax[]) enumTables[i][1];
            }
        }
        return null;
    }

    static Chromaticity[] enumChromaticityTable = { Chromaticity.MONOCHROME,
            Chromaticity.COLOR };

    static ColorSupported[] enumColorSupportedTable = { ColorSupported.NOT_SUPPORTED,
            ColorSupported.SUPPORTED };

    static Compression[] enumCompressionTable = { Compression.NONE,
            Compression.DEFLATE,
            Compression.GZIP,
            Compression.COMPRESS };

    static Fidelity[] enumFidelityTable = { Fidelity.FIDELITY_TRUE,
            Fidelity.FIDELITY_FALSE };

    static Finishings[] enumFinishingsTable = { Finishings.NONE,
            Finishings.STAPLE,
            null,
            Finishings.COVER,
            Finishings.BIND,
            Finishings.SADDLE_STITCH,
            Finishings.EDGE_STITCH,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Finishings.STAPLE_TOP_LEFT,
            Finishings.STAPLE_BOTTOM_LEFT,
            Finishings.STAPLE_TOP_RIGHT,
            Finishings.STAPLE_BOTTOM_RIGHT,
            Finishings.EDGE_STITCH_LEFT,
            Finishings.EDGE_STITCH_TOP,
            Finishings.EDGE_STITCH_RIGHT,
            Finishings.EDGE_STITCH_BOTTOM,
            Finishings.STAPLE_DUAL_LEFT,
            Finishings.STAPLE_DUAL_TOP,
            Finishings.STAPLE_DUAL_RIGHT,
            Finishings.STAPLE_DUAL_BOTTOM };

    static JobSheets[] enumJobSheetsTable = { JobSheets.NONE,
            JobSheets.STANDARD };

    static JobState[] enumJobStateTable = { JobState.UNKNOWN,
            null,
            null,
            JobState.PENDING,
            JobState.PENDING_HELD,
            JobState.PROCESSING,
            JobState.PROCESSING_STOPPED,
            JobState.CANCELED,
            JobState.ABORTED,
            JobState.COMPLETED };

    static JobStateReason[] enumJobStateReasonTable = { JobStateReason.JOB_INCOMING,
            JobStateReason.JOB_DATA_INSUFFICIENT,
            JobStateReason.DOCUMENT_ACCESS_ERROR,
            JobStateReason.SUBMISSION_INTERRUPTED,
            JobStateReason.JOB_OUTGOING,
            JobStateReason.JOB_HOLD_UNTIL_SPECIFIED,
            JobStateReason.RESOURCES_ARE_NOT_READY,
            JobStateReason.PRINTER_STOPPED_PARTLY,
            JobStateReason.PRINTER_STOPPED,
            JobStateReason.JOB_INTERPRETING,
            JobStateReason.JOB_QUEUED,
            JobStateReason.JOB_TRANSFORMING,
            JobStateReason.JOB_QUEUED_FOR_MARKER,
            JobStateReason.JOB_PRINTING,
            JobStateReason.JOB_CANCELED_BY_USER,
            JobStateReason.JOB_CANCELED_BY_OPERATOR,
            JobStateReason.JOB_CANCELED_AT_DEVICE,
            JobStateReason.ABORTED_BY_SYSTEM,
            JobStateReason.UNSUPPORTED_COMPRESSION,
            JobStateReason.COMPRESSION_ERROR,
            JobStateReason.UNSUPPORTED_DOCUMENT_FORMAT,
            JobStateReason.DOCUMENT_FORMAT_ERROR,
            JobStateReason.PROCESSING_TO_STOP_POINT,
            JobStateReason.SERVICE_OFF_LINE,
            JobStateReason.JOB_COMPLETED_SUCCESSFULLY,
            JobStateReason.JOB_COMPLETED_WITH_WARNINGS,
            JobStateReason.JOB_COMPLETED_WITH_ERRORS,
            JobStateReason.JOB_RESTARTABLE,
            JobStateReason.QUEUED_IN_DEVICE };

    static MediaName[] enumMediaNameTable = { MediaName.NA_LETTER_WHITE,
            MediaName.NA_LETTER_TRANSPARENT,
            MediaName.ISO_A4_WHITE,
            MediaName.ISO_A4_TRANSPARENT };

    static MediaSizeName[] enumMediaSizeNameTable = { MediaSizeName.ISO_A0,
            MediaSizeName.ISO_A1,
            MediaSizeName.ISO_A2,
            MediaSizeName.ISO_A3,
            MediaSizeName.ISO_A4,
            MediaSizeName.ISO_A5,
            MediaSizeName.ISO_A6,
            MediaSizeName.ISO_A7,
            MediaSizeName.ISO_A8,
            MediaSizeName.ISO_A9,
            MediaSizeName.ISO_A10,
            MediaSizeName.ISO_B0,
            MediaSizeName.ISO_B1,
            MediaSizeName.ISO_B2,
            MediaSizeName.ISO_B3,
            MediaSizeName.ISO_B4,
            MediaSizeName.ISO_B5,
            MediaSizeName.ISO_B6,
            MediaSizeName.ISO_B7,
            MediaSizeName.ISO_B8,
            MediaSizeName.ISO_B9,
            MediaSizeName.ISO_B10,
            MediaSizeName.JIS_B0,
            MediaSizeName.JIS_B1,
            MediaSizeName.JIS_B2,
            MediaSizeName.JIS_B3,
            MediaSizeName.JIS_B4,
            MediaSizeName.JIS_B5,
            MediaSizeName.JIS_B6,
            MediaSizeName.JIS_B7,
            MediaSizeName.JIS_B8,
            MediaSizeName.JIS_B9,
            MediaSizeName.JIS_B10,
            MediaSizeName.ISO_C0,
            MediaSizeName.ISO_C1,
            MediaSizeName.ISO_C2,
            MediaSizeName.ISO_C3,
            MediaSizeName.ISO_C4,
            MediaSizeName.ISO_C5,
            MediaSizeName.ISO_C6,
            MediaSizeName.NA_LETTER,
            MediaSizeName.NA_LEGAL,
            MediaSizeName.EXECUTIVE,
            MediaSizeName.LEDGER,
            MediaSizeName.TABLOID,
            MediaSizeName.INVOICE,
            MediaSizeName.FOLIO,
            MediaSizeName.QUARTO,
            MediaSizeName.JAPANESE_POSTCARD,
            MediaSizeName.JAPANESE_DOUBLE_POSTCARD,
            MediaSizeName.A,
            MediaSizeName.B,
            MediaSizeName.C,
            MediaSizeName.D,
            MediaSizeName.E,
            MediaSizeName.ISO_DESIGNATED_LONG,
            MediaSizeName.ITALY_ENVELOPE,
            MediaSizeName.MONARCH_ENVELOPE,
            MediaSizeName.PERSONAL_ENVELOPE,
            MediaSizeName.NA_NUMBER_9_ENVELOPE,
            MediaSizeName.NA_NUMBER_10_ENVELOPE,
            MediaSizeName.NA_NUMBER_11_ENVELOPE,
            MediaSizeName.NA_NUMBER_12_ENVELOPE,
            MediaSizeName.NA_NUMBER_14_ENVELOPE,
            MediaSizeName.NA_6X9_ENVELOPE,
            MediaSizeName.NA_7X9_ENVELOPE,
            MediaSizeName.NA_9X11_ENVELOPE,
            MediaSizeName.NA_9X12_ENVELOPE,
            MediaSizeName.NA_10X13_ENVELOPE,
            MediaSizeName.NA_10X14_ENVELOPE,
            MediaSizeName.NA_10X15_ENVELOPE,
            MediaSizeName.NA_5X7,
            MediaSizeName.NA_8X10 };

    static MediaTray[] enumMediaTrayTable = { MediaTray.TOP,
            MediaTray.MIDDLE,
            MediaTray.BOTTOM,
            MediaTray.ENVELOPE,
            MediaTray.MANUAL,
            MediaTray.LARGE_CAPACITY,
            MediaTray.MAIN,
            MediaTray.SIDE };

    static MultipleDocumentHandling[] enumMultipleDocumentHandlingTable = { MultipleDocumentHandling.SINGLE_DOCUMENT,
            MultipleDocumentHandling.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES,
            MultipleDocumentHandling.SEPARATE_DOCUMENTS_COLLATED_COPIES,
            MultipleDocumentHandling.SINGLE_DOCUMENT_NEW_SHEET };

    static OrientationRequested[] enumOrientationRequestedTable = { OrientationRequested.PORTRAIT,
            OrientationRequested.LANDSCAPE,
            OrientationRequested.REVERSE_LANDSCAPE,
            OrientationRequested.REVERSE_PORTRAIT };

    static PDLOverrideSupported[] enumPDLOverrideSupportedTable = { PDLOverrideSupported.NOT_ATTEMPTED,
            PDLOverrideSupported.ATTEMPTED };

    static PresentationDirection[] enumPresentationDirectionTable = { PresentationDirection.TOBOTTOM_TORIGHT,
            PresentationDirection.TOBOTTOM_TOLEFT,
            PresentationDirection.TOTOP_TORIGHT,
            PresentationDirection.TOTOP_TOLEFT,
            PresentationDirection.TORIGHT_TOBOTTOM,
            PresentationDirection.TORIGHT_TOTOP,
            PresentationDirection.TOLEFT_TOBOTTOM,
            PresentationDirection.TOLEFT_TOTOP };

    static PrinterIsAcceptingJobs[] enumPrinterIsAcceptingJobsTable = { PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS,
            PrinterIsAcceptingJobs.ACCEPTING_JOBS };

    static PrinterState[] enumPrinterStateTable = { PrinterState.UNKNOWN,
            null,
            null,
            PrinterState.IDLE,
            PrinterState.PROCESSING,
            PrinterState.STOPPED };

    static PrinterStateReason[] enumPrinterStateReasonTable = { PrinterStateReason.OTHER,
            PrinterStateReason.MEDIA_NEEDED,
            PrinterStateReason.MEDIA_JAM,
            PrinterStateReason.MOVING_TO_PAUSED,
            PrinterStateReason.PAUSED,
            PrinterStateReason.SHUTDOWN,
            PrinterStateReason.CONNECTING_TO_DEVICE,
            PrinterStateReason.TIMED_OUT,
            PrinterStateReason.STOPPING,
            PrinterStateReason.STOPPED_PARTLY,
            PrinterStateReason.TONER_LOW,
            PrinterStateReason.TONER_EMPTY,
            PrinterStateReason.SPOOL_AREA_FULL,
            PrinterStateReason.COVER_OPEN,
            PrinterStateReason.INTERLOCK_OPEN,
            PrinterStateReason.DOOR_OPEN,
            PrinterStateReason.INPUT_TRAY_MISSING,
            PrinterStateReason.MEDIA_LOW,
            PrinterStateReason.MEDIA_EMPTY,
            PrinterStateReason.OUTPUT_TRAY_MISSING,
            PrinterStateReason.OUTPUT_AREA_ALMOST_FULL,
            PrinterStateReason.OUTPUT_AREA_FULL,
            PrinterStateReason.MARKER_SUPPLY_LOW,
            PrinterStateReason.MARKER_SUPPLY_EMPTY,
            PrinterStateReason.MARKER_WASTE_ALMOST_FULL,
            PrinterStateReason.MARKER_WASTE_FULL,
            PrinterStateReason.FUSER_OVER_TEMP,
            PrinterStateReason.FUSER_UNDER_TEMP,
            PrinterStateReason.OPC_NEAR_EOL,
            PrinterStateReason.OPC_LIFE_OVER,
            PrinterStateReason.DEVELOPER_LOW,
            PrinterStateReason.DEVELOPER_EMPTY,
            PrinterStateReason.INTERPRETER_RESOURCE_UNAVAILABLE };

    static PrintQuality[] enumPrintQualityTable = { PrintQuality.DRAFT,
            PrintQuality.NORMAL,
            PrintQuality.HIGH };

    static ReferenceUriSchemesSupported[] enumReferenceUriSchemesSupportedTable = { ReferenceUriSchemesSupported.FTP,
            ReferenceUriSchemesSupported.HTTP,
            ReferenceUriSchemesSupported.HTTPS,
            ReferenceUriSchemesSupported.GOPHER,
            ReferenceUriSchemesSupported.NEWS,
            ReferenceUriSchemesSupported.NNTP,
            ReferenceUriSchemesSupported.WAIS,
            ReferenceUriSchemesSupported.FILE };

    static Severity[] enumSeverityTable = { Severity.REPORT,
            Severity.WARNING,
            Severity.ERROR };

    static SheetCollate[] enumSheetCollateTable = { SheetCollate.UNCOLLATED,
            SheetCollate.COLLATED };

    static Sides[] enumSidesTable = { Sides.ONE_SIDED,
            Sides.TWO_SIDED_LONG_EDGE,
            Sides.TWO_SIDED_SHORT_EDGE };

    static PPDMediaSizeName[] enumPPDMediaSizeNameTable = (PPDMediaSizeName[]) PPDMediaSizeName.A4
            .getEnumValueTable();

    static Object[][] enumTables = { { Chromaticity.class,
            enumChromaticityTable },
            { ColorSupported.class, enumColorSupportedTable },
            { Compression.class, enumCompressionTable },
            { Fidelity.class, enumFidelityTable },
            { Finishings.class, enumFinishingsTable },
            { JobSheets.class, enumJobSheetsTable },
            { JobState.class, enumJobStateTable },
            { JobStateReason.class, enumJobStateReasonTable },
            { MediaName.class, enumMediaNameTable },
            { MediaSizeName.class, enumMediaSizeNameTable },
            { MediaTray.class, enumMediaTrayTable },
            { MultipleDocumentHandling.class, enumMultipleDocumentHandlingTable },
            { OrientationRequested.class, enumOrientationRequestedTable },
            { PDLOverrideSupported.class, enumPDLOverrideSupportedTable },
            { PresentationDirection.class, enumPresentationDirectionTable },
            { PrinterIsAcceptingJobs.class, enumPrinterIsAcceptingJobsTable },
            { PrinterState.class, enumPrinterStateTable },
            { PrinterStateReason.class, enumPrinterStateReasonTable },
            { PrintQuality.class, enumPrintQualityTable },
            { ReferenceUriSchemesSupported.class,
                    enumReferenceUriSchemesSupportedTable },
            { Severity.class, enumSeverityTable },
            { SheetCollate.class, enumSheetCollateTable },
            { Sides.class, enumSidesTable },
            { PPDMediaSizeName.class, enumPPDMediaSizeNameTable } };

}

