package org.jivesoftware.smackx.jingle.media;

/**
 * Represents a payload type.
 *
 * @author Alvaro Saurin
 */
public class PayloadType {

    public static int MAX_FIXED_PT = 95;

    public static int INVALID_PT = 65535;

    private int id;

    private String name;

    private int channels;

    /**
     * Constructor with Id, name and number of channels
     *
     * @param id       The identifier
     * @param name     A name
     * @param channels The number of channels
     */
    public PayloadType(int id, final String name, int channels) {
        super();
        this.id = id;
        this.name = name;
        this.channels = channels;
    }

    /**
     * Default constructor.
     */
    public PayloadType() {
        this(INVALID_PT, null, 1);
    }

    /**
     * Constructor with Id and name
     *
     * @param id   The identification
     * @param name A name
     */
    public PayloadType(int id, String name) {
        this(id, name, 1);
    }

    /**
     * Copy constructor
     *
     * @param pt The other payload type.
     */
    public PayloadType(PayloadType pt) {
        this(pt.getId(), pt.getName(), pt.getChannels());
    }

    /**
     * Get the ID.
     *
     * @return the ID
     */
    public int getId() {
        return id;
    }

    /**
     * Set the ID.
     *
     * @param id ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get the printable name.
     *
     * @return printable name for the payload type
     */
    public String getName() {
        return name;
    }

    /**
     * Set the printable name.
     *
     * @param name the printable name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the number of channels used by this payload type.
     *
     * @return the number of channels
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Set the numer of channels for a payload type.
     *
     * @param channels The number of channels
     */
    public void setChannels(int channels) {
        this.channels = channels;
    }

    /**
     * Return true if the Payload type is not valid
     *
     * @return true if the payload type is invalid
     */
    public boolean isNull() {
        if (getId() == INVALID_PT) {
            return true;
        } else if (getName() == null) {
            return true;
        }
        return false;
    }

    /*
      * (non-Javadoc)
      *
      * @see java.lang.Object#hashCode()
      */
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + getChannels();
        result = PRIME * result + getId();
        result = PRIME * result + (getName() == null ? 0 : getName().hashCode());
        return result;
    }

    /*
      * (non-Javadoc)
      *
      * @see java.lang.Object#equals(java.lang.Object)
      */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PayloadType other = (PayloadType) obj;
        if (getChannels() != other.getChannels()) {
            return false;
        }
        if (getId() != other.getId()) {
            return false;
        }

        // Compare names only for dynamic payload types
        if (getId() > MAX_FIXED_PT) {
            if (getName() == null) {
                if (other.getName() != null) {
                    return false;
                }
            } else if (!getName().equals(other.getName())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Audio payload type.
     */
    public static class Audio extends PayloadType {
        private int clockRate;

        /**
         * Constructor with all the attributes of an Audio payload type
         *
         * @param id       The identifier
         * @param name     The name assigned to this payload type
         * @param channels The number of channels
         * @param rate     The clock rate
         */
        public Audio(int id, String name, int channels, int rate) {
            super(id, name, channels);
            clockRate = rate;
        }

        /**
         * Empty constructor.
         */
        public Audio() {
            super();
            clockRate = 0;
        }

        /**
         * Constructor with Id and name
         *
         * @param id   the Id for the payload type
         * @param name the name of the payload type
         */
        public Audio(int id, String name) {
            super(id, name);
            clockRate = 0;
        }

        /**
         * Copy constructor
         *
         * @param pt the other payload type
         */
        public Audio(PayloadType pt) {
            super(pt);
            clockRate = 0;
        }

        /**
         * Copy constructor
         *
         * @param pt the other payload type
         */
        public Audio(PayloadType.Audio pt) {
            super(pt);
            clockRate = pt.getClockRate();
        }

        /**
         * Get the sampling clockRate for a payload type
         *
         * @return The sampling clockRate
         */
        public int getClockRate() {
            return clockRate;
        }

        /**
         * Set tha sampling clockRate for a playload type.
         *
         * @param rate The sampling clockRate
         */
        public void setClockRate(int rate) {
            clockRate = rate;
        }

        /*
           * (non-Javadoc)
           *
           * @see java.lang.Object#hashCode()
           */
        public int hashCode() {
            final int PRIME = 31;
            int result = super.hashCode();
            result = PRIME * result + getClockRate();
            return result;
        }

        /*
           * (non-Javadoc)
           *
           * @see java.lang.Object#equals(java.lang.Object)
           */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Audio other = (Audio) obj;
            if (getClockRate() != other.getClockRate()) {
                return false;
            }
            return true;
        }
    }
}