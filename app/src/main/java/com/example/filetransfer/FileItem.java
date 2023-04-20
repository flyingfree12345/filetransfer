package com.example.filetransfer;

import java.io.File;

public class FileItem {

    private File itemFile;
    private boolean itemChecked;

    public FileItem(File f, boolean c)
    {
        itemFile = f;
        itemChecked = c;
    }

    public File getFile()
    {
        return itemFile;
    }
    public void setChecked(boolean c)
    {
        itemChecked = c;
    }
    public boolean getChecked()
    {
        return itemChecked;
    }
}
