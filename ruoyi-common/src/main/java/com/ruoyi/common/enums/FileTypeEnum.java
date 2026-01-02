package com.ruoyi.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
@AllArgsConstructor
public enum FileTypeEnum {
    MAP4(".mp4", "icon-shipinicon"),
    FLV(".flv", "icon-shipinicon"),
    AVI(".avi", "icon-shipinicon"),
    MKV(".mkv", "icon-shipinicon"),
    MOV(".mov", "icon-shipinicon"),
    WMV(".wmv", "icon-shipinicon"),
    PDF(".pdf", "icon-pdfwendangicon"),
    DOC(".doc", "icon-docwendangicon"),
    DOCX(".docx", "icon-docwendangicon"),
    XLS(".xls", "icon-xlswendangicon"),
    XLSX(".xlsx", "icon-xlswendangicon"),
    PPT(".ppt", "icon-pptwendangicon"),
    PPTX(".pptx", "icon-pptwendangicon"),
    RAR(".rar", "icon-yasuobaowendangicon"),
    ZIP(".zip", "icon-yasuobaowendangicon"),
    MP3(".mp3", "icon-yinpinicon"),
    WAV(".wav", "icon-yinpinicon"),
    WMA(".wma", "icon-yinpinicon"),
    JPG(".jpg", "icon-tupianicon"),
    JPEG(".jpeg", "icon-tupianicon"),
    PNG(".png", "icon-tupianicon"),
    BMP(".bmp", "icon-tupianicon"),
    GIF(".gif", "icon-tupianicon");

    private String name;
    private String icon;

    public static boolean isVideo(String fileName) {
        return fileName.toLowerCase().contains(MAP4.getName()) ||
                fileName.toLowerCase().contains(FLV.getName()) ||
                fileName.toLowerCase().contains(AVI.getName()) ||
                fileName.toLowerCase().contains(MKV.getName()) ||
                fileName.toLowerCase().contains(MOV.getName()) ||
                fileName.toLowerCase().contains(WMV.getName());
    }

    public static boolean isAudio(String fileName) {
        return fileName.toLowerCase().contains(MP3.getName()) ||
                fileName.toLowerCase().contains(WAV.getName()) ||
                fileName.toLowerCase().contains(WMA.getName());
    }

    public static boolean isDoc(String fileName) {
        return fileName.toLowerCase().contains(PDF.getName()) ||
                fileName.toLowerCase().contains(DOC.getName()) ||
                fileName.toLowerCase().contains(DOCX.getName()) ||
                fileName.toLowerCase().contains(XLS.getName()) ||
                fileName.toLowerCase().contains(XLSX.getName()) ||
                fileName.toLowerCase().contains(PPT.getName()) ||
                fileName.toLowerCase().contains(PPTX.getName());
    }

    public static boolean isSupportedType(String fileName) {
        return fileName.toLowerCase().contains(MAP4.getName()) ||
                fileName.toLowerCase().contains(FLV.getName()) ||
                fileName.toLowerCase().contains(AVI.getName()) ||
                fileName.toLowerCase().contains(MKV.getName()) ||
                fileName.toLowerCase().contains(MOV.getName()) ||
                fileName.toLowerCase().contains(WMV.getName()) ||
                fileName.toLowerCase().contains(PDF.getName()) ||
                fileName.toLowerCase().contains(DOC.getName()) ||
                fileName.toLowerCase().contains(DOCX.getName()) ||
                fileName.toLowerCase().contains(XLS.getName()) ||
                fileName.toLowerCase().contains(XLSX.getName()) ||
                fileName.toLowerCase().contains(PPT.getName()) ||
                fileName.toLowerCase().contains(PPTX.getName()) ||
                fileName.toLowerCase().contains(RAR.getName()) ||
                fileName.toLowerCase().contains(ZIP.getName()) ||
                fileName.toLowerCase().contains(MP3.getName()) ||
                fileName.toLowerCase().contains(WAV.getName()) ||
                fileName.toLowerCase().contains(WMA.getName()) ||
                fileName.toLowerCase().contains(JPG.getName()) ||
                fileName.toLowerCase().contains(JPEG.getName()) ||
                fileName.toLowerCase().contains(PNG.getName()) ||
                fileName.toLowerCase().contains(BMP.getName()) ||
                fileName.toLowerCase().contains(GIF.getName());
    }

    public static MaterialTypeEnum getMaterialType(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        if (fileName.toLowerCase().contains(MAP4.getName()) ||
                fileName.toLowerCase().contains(FLV.getName()) ||
                fileName.toLowerCase().contains(AVI.getName()) ||
                fileName.toLowerCase().contains(MKV.getName()) ||
                fileName.toLowerCase().contains(MOV.getName()) ||
                fileName.toLowerCase().contains(WMV.getName())) {
            return MaterialTypeEnum.FILE_TYPE_VIDEO;
        }
        if (fileName.toLowerCase().contains(PDF.getName()) ||
                fileName.toLowerCase().contains(DOC.getName()) ||
                fileName.toLowerCase().contains(DOCX.getName()) ||
                fileName.toLowerCase().contains(XLS.getName()) ||
                fileName.toLowerCase().contains(XLSX.getName()) ||
                fileName.toLowerCase().contains(PPT.getName()) ||
                fileName.toLowerCase().contains(PPTX.getName())) {
            return MaterialTypeEnum.FILE_TYPE_DOC;
        }
        if (fileName.toLowerCase().contains(RAR.getName()) ||
                fileName.toLowerCase().contains(ZIP.getName())) {
            return MaterialTypeEnum.FILE_TYPE_PAC;
        }
        if (fileName.toLowerCase().contains(MP3.getName()) ||
                fileName.toLowerCase().contains(WAV.getName()) ||
                fileName.toLowerCase().contains(WMA.getName())) {
            return MaterialTypeEnum.FILE_TYPE_AUDIO;
        }
        if (fileName.toLowerCase().contains(JPG.getName()) ||
                fileName.toLowerCase().contains(JPEG.getName()) ||
                fileName.toLowerCase().contains(PNG.getName()) ||
                fileName.toLowerCase().contains(BMP.getName()) ||
                fileName.toLowerCase().contains(GIF.getName())) {
            return MaterialTypeEnum.FILE_TYPE_PIC;
        }
        return null;
    }

    public static MaterialTypeEnum isOnlyMedia(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        if (fileName.toLowerCase().contains(MAP4.getName()) ||
                fileName.toLowerCase().contains(FLV.getName()) ||
                fileName.toLowerCase().contains(AVI.getName()) ||
                fileName.toLowerCase().contains(MKV.getName()) ||
                fileName.toLowerCase().contains(MOV.getName()) ||
                fileName.toLowerCase().contains(WMV.getName())) {
            return MaterialTypeEnum.FILE_TYPE_VIDEO;
        }
        if (fileName.toLowerCase().contains(MP3.getName()) ||
                fileName.toLowerCase().contains(WAV.getName()) ||
                fileName.toLowerCase().contains(WMA.getName())) {
            return MaterialTypeEnum.FILE_TYPE_AUDIO;
        }
        return null;
    }

    public static Boolean isOnlyPdf(List<String> path) {
        boolean isOnlyPdf = true;
        for (String s : path) {
            if (!s.contains(PDF.getName())) {
                isOnlyPdf = false;
                break;
            }
        }
        return isOnlyPdf;
    }

    public static FileTypeEnum getByCode(String extension) {
        for (FileTypeEnum fileTypeEnum : FileTypeEnum.values()) {
            if (fileTypeEnum.getName().contains(extension)) {
                return fileTypeEnum;
            }
        }
        return null;
    }
}
