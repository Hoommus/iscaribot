package wikia;

import model.annotations.UserField;
import model.dbfields.UserFields;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class WikiaUser {
    @UserField(inDB = UserFields.wikiaid)
    private Long userid;
    @UserField(inDB = UserFields.wikianame)
    private String name;
    @UserField(inDB = UserFields.NULL)
    private int editcount;
    @UserField(inDB = UserFields.NULL)
    private Instant registration;
    @UserField(inDB = UserFields.NULL)
    private String[] groups;
    
    @UserField(inDB = UserFields.NULL)
    private String avatarUrl;
    
    @UserField(inDB = UserFields.NULL)
    private Instant lastUpdate;
    
    public WikiaUser() {

    }

    public WikiaUser(long userid, String name, int editcount, Instant registration, String[] groups) {
        this.userid = userid;
        this.name = name;
        this.editcount = editcount;
        this.registration = registration;
        this.groups = groups;
    }
    
    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        WikiaUser wikiaUser = (WikiaUser) o;
        
        if (editcount != wikiaUser.editcount) return false;
        if (userid != null ? !userid.equals(wikiaUser.userid) : wikiaUser.userid != null) return false;
        if (name != null ? !name.equals(wikiaUser.name) : wikiaUser.name != null) return false;
        if (registration != null ? !registration.equals(wikiaUser.registration) : wikiaUser.registration != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(groups, wikiaUser.groups)) return false;
        return avatarUrl != null ? avatarUrl.equals(wikiaUser.avatarUrl) : wikiaUser.avatarUrl == null;
    }
    
    @Override
    public int hashCode() {
        int result = (int) (userid ^ (userid >>> 32));
        result = 31 * result + name.hashCode();
        result = 31 * result + editcount;
        result = 31 * result + registration.hashCode();
        result = 31 * result + Arrays.hashCode(groups);
        return result;
    }
    
    public String getGenericWikiaURL() {
        return "http://community.wikia.com/wiki/User:" + name;
    }
    
    public Long getUserid() {
        return userid;
    }

    public String getName() {
        return name;
    }
    
    public int getEditcount() {
        return editcount;
    }

    public String[] getGroups() {
        return groups;
    }
    
    public List<String> getGroupsList() {
        return new ArrayList<>(Arrays.asList(groups));
    }
    

    public Instant getRegistration() {
        return registration;
    }

    public WikiaUser setUserid(Long userid) {
        this.userid = userid;
        return this;
    }

    public WikiaUser setName(String name) {
        this.name = name;
        return this;
    }

    public WikiaUser setEditcount(int editcount) {
        if(editcount < 0)
            throw new IllegalArgumentException("editcount might not be less than zero.");
        this.editcount = editcount;
        return this;
    }

    public WikiaUser setRegistration(Instant registration) {
        this.registration = registration;
        return this;
    }

    public WikiaUser setGroups(String[] groups) {
        this.groups = groups;
        return this;
    }

    @Override
    public String toString() {
        return "WikiaUser{" +
                "userid=" + userid +
                ", name='" + name + '\'' +
                ", editcount=" + editcount +
                ", registration='" + registration + '\'' +
                ", groups=" + Arrays.toString(groups) +
                '}';
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public WikiaUser setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }
    
    public Instant getLastUpdate() {
        return lastUpdate;
    }
    
    public WikiaUser setLastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }
}
