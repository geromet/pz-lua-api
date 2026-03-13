/*
 * Decompiled with CFR 0.152.
 */
package zombie.text.templating;

import java.util.Objects;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.text.templating.IReplace;
import zombie.text.templating.ReplaceProvider;

@UsedFromLua
public class ReplaceProviderCharacter
extends ReplaceProvider {
    public ReplaceProviderCharacter(final IsoGameCharacter character) {
        this.addReplacer("firstname", new IReplace(){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public String getString() {
                if (character != null && character.getDescriptor() != null && character.getDescriptor().getForename() != null) {
                    return character.getDescriptor().getForename();
                }
                return "Bob";
            }
        });
        this.addReplacer("lastname", new IReplace(){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public String getString() {
                if (character != null && character.getDescriptor() != null && character.getDescriptor().getSurname() != null) {
                    return character.getDescriptor().getSurname();
                }
                return "Smith";
            }
        });
    }
}

