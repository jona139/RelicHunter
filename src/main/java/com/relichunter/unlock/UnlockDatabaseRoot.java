// Filename: RelicHunter/src/main/java/com/relichunter/unlock/UnlockDatabaseRoot.java
// NEW FILE: Represents the root structure of the unlock database JSON
package com.relichunter.unlock;

import lombok.Data;
import java.util.List;

@Data
public class UnlockDatabaseRoot {
    private List<UnlockData> unlocks;
}
