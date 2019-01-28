package de.arcoiris;

import java.util.*;
import java.io.*;

public class FileComparator implements Comparator
{
    public static final int SORT_BY_FILENAME=1; 
    public static final int SORT_BY_CASESENSITIVE=2; 
    public static final int SORT_BY_EXTENSION=3; 
    public static final int SORT_BY_SIZE=4; 
    public static final int SORT_BY_DATE=5; 
    
    int sortBy;

    public FileComparator(int sortBy)
    {
        this.sortBy = sortBy;
    }

    public int compare(Object o1, Object o2)
    {
        if (!o2.getClass().equals(o1.getClass()))
        {
            throw new ClassCastException();
        }
        
        File file1 = (File) o1;
        File file2 = (File) o2;

        String fileName1 = file1.getName();
        String fileName2 = file2.getName();

        if (sortBy == SORT_BY_FILENAME)
        {
            return(fileName1.toUpperCase().compareTo(fileName2.toUpperCase()));
        }

        if (sortBy == SORT_BY_CASESENSITIVE)
        {
            return(fileName1.compareTo(fileName2));
        }

        if (sortBy == SORT_BY_EXTENSION)
        {
            String ext1="";
            String ext2="";

            int extIdx=fileName1.lastIndexOf(".");
            if (extIdx>=0)
            {
                ext1=fileName1.substring(extIdx);
            }
            
            extIdx = fileName2.lastIndexOf(".");
            if (extIdx >= 0)
            {
                ext2=fileName2.substring(extIdx);
            }

            return(ext1.toUpperCase().compareTo(ext2.toUpperCase()));
        }

        if (sortBy==SORT_BY_SIZE)
        {
            long fileSize1 = file1.length();

            long fileSize2 = file2.length();

            if (fileSize1 < fileSize2)
            {
                return(1);
            }
            
            if (fileSize1 > fileSize2)
            {
                return(-1);
            }

            return(0);
        }
        
        if (sortBy == SORT_BY_DATE)
        {
            long fileDate1 = file1.lastModified();

            long fileDate2 = file2.lastModified();

            if (fileDate1 < fileDate2)
            {
                return(1);
            }
            
            if (fileDate1 > fileDate2)
            {
                return(-1);
            }

            return 0;
        }
        
        return 0;
    }
 
    /**
     * returns true only if  obj == this
     */
    public boolean equals(Object obj)
    {
        return obj.equals(this);
    }
}