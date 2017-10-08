package de.arcoiris;

import java.io.File;
import java.util.Comparator;
import java.util.Date;

public class OfflineQueueComparator implements Comparator
{
    public static final int SORT_BY_DATE = 1;

    int sortBy;

    public OfflineQueueComparator(int sortBy)
    {
        this.sortBy = sortBy;
    }

    public int compare(Object o1, Object o2) {
        if (!o2.getClass().equals(o1.getClass())) {
            throw new ClassCastException();
        }
        
        OfflineQueueEntryInfo entry1 = (OfflineQueueEntryInfo) o1;
        OfflineQueueEntryInfo entry2 = (OfflineQueueEntryInfo) o2;

        if (entry1.getMetaData() == null) {
            if (entry2.getMetaData() == null) {
                return 0;
            } else {
                return 1;
            }
        } else {
            if (entry2.getMetaData() == null) {
                return -1;
            }
        }

        if (sortBy == SORT_BY_DATE) {
            Date date1 = entry1.getMetaData().getBlogDate();
            Date date2 = entry2.getMetaData().getBlogDate();

            if (date1 == null) {
                if (date2 == null) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                if (date2 == null) {
                    return -1;
                }
            }

            if (date1.getTime() < date2.getTime()) {
                return -1;
            }
            if (date1.getTime() > date2.getTime()) {
                return 1;
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