/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.ArrayList;
import org.joml.Vector3f;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.scripting.objects.IModelAttachmentOwner;
import zombie.util.StringUtils;

@UsedFromLua
public final class ModelAttachment {
    private IModelAttachmentOwner owner;
    private String id;
    private final Vector3f offset = new Vector3f();
    private final Vector3f rotate = new Vector3f();
    private float scale = 1.0f;
    private String bone;
    private ArrayList<String> canAttach;
    private float zoffset;
    private boolean updateConstraint = true;

    public ModelAttachment(String id) {
        this.setId(id);
    }

    public void setOwner(IModelAttachmentOwner owner) {
        this.owner = owner;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        if (StringUtils.isNullOrWhitespace(id)) {
            throw new IllegalArgumentException("ModelAttachment id is null or empty");
        }
        if (this.owner != null) {
            this.owner.beforeRenameAttachment(this);
        }
        this.id = id;
        if (this.owner != null) {
            this.owner.afterRenameAttachment(this);
        }
    }

    public Vector3f getOffset() {
        return this.offset;
    }

    public Vector3f getRotate() {
        return this.rotate;
    }

    public float getScale() {
        return this.scale;
    }

    public void setScale(float scale) {
        this.scale = PZMath.max(scale, 0.01f);
    }

    public String getBone() {
        return this.bone;
    }

    public void setBone(String bone) {
        this.bone = (bone = bone.trim()).isEmpty() ? null : bone;
    }

    public ArrayList<String> getCanAttach() {
        return this.canAttach;
    }

    public void setCanAttach(ArrayList<String> canAttach) {
        this.canAttach = canAttach;
    }

    public float getZOffset() {
        return this.zoffset;
    }

    public void setZOffset(float zoffset) {
        this.zoffset = zoffset;
    }

    public boolean isUpdateConstraint() {
        return this.updateConstraint;
    }

    public void setUpdateConstraint(boolean updateConstraint) {
        this.updateConstraint = updateConstraint;
    }
}

