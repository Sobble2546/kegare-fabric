/**
 * Kegare's addon API: the only part of Kegare that addons should use. Everything outside {@code com.cnethuka.kegare.api}
 * is internal and may change in any release.
 * <p>
 * <b>Experimental until Kegare 1.0.</b> Within one minor version (0.2.0, 0.2.1, ...) nothing here changes in a way that
 * breaks an addon; a new minor version (0.3.0) may. Declare your Kegare dependency as one minor range, for example
 * {@code versionRange = "[0.2,0.3)"}.
 * <p>
 * Start at {@link com.cnethuka.kegare.api.KegareApi}. The guide is {@code docs/addon-api.md} in Kegare's repository.
 */
@ApiStatus.Experimental
package com.cnethuka.kegare.api;

import org.jetbrains.annotations.ApiStatus;
