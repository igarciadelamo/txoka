package es.igarciadelamo.txoka.review;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Ephemeral review helper: no production component or application permission changes. */
@RunWith(AndroidJUnit4.class)
public final class NativeReviewScreenshotsTest {
    private static final String APP = "es.igarciadelamo.txoka";
    private static final String APP_SOURCE = "4692fbc65a988a64201fea886286aaf93288882c";
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();
    private final UiAutomation automation = instrumentation.getUiAutomation();
    private final JSONArray scenes = new JSONArray();
    private final JSONObject probes = new JSONObject();
    private final File output = new File(context.getFilesDir(), "review-screenshots");
    private int screenWidth = 1080;
    private int screenHeight = 2400;

    @Test
    public void captureNativeReviewScenes() throws Exception {
        if (!output.isDirectory() && !output.mkdirs()) throw new IllegalStateException("Cannot create screenshot directory");
        String failure = null;
        try {
            if (!APP_SOURCE.matches("[a-f0-9]{40}")) throw new IllegalStateException("App source SHA was not bound");
            shell("settings put system accelerometer_rotation 0");
            shell("settings put system user_rotation 0");
            shell("cmd uimode night no");
            probes.put("masks", diagnoseMasks());
            write("mask-diagnostics.json", probes.getJSONObject("masks").toString(2).getBytes(StandardCharsets.UTF_8));
            instrumentation.runOnMainSync(() -> context.startActivity(
                    new Intent().setClassName(APP, APP + ".MainActivity")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)));
            waitFor("Marta Ruiz", false, 30000, true);
            // Allow the actual-size SVG mask to finish its first asynchronous render.
            SystemClock.sleep(12000);
            capture("01-hub-hologram", "Native hub with a holographic fish mosaic and sample contact details", false,
                    "View card full screen");

            tap("Edit card", true);
            tap("Mosaic", false);
            tapWithScroll("None");
            tap("Background", false);
            capture("02-editor-material-options", "All six background choices in the actual native editor", false,
                    "Stained glass", "Nacre", "Brushed metal", "Save");
            tapWithScroll("Stained glass");
            capture("03-editor-stained-glass", "Bundled glass relief and native stained-glass lighting", false,
                    "Stained glass", "Save");
            tapWithScroll("Nacre");
            capture("04-editor-nacre", "Native nacre material with the complete card preview", false,
                    "Nacre", "Save");
            tapWithScroll("Brushed metal");
            capture("05-editor-brushed-metal", "Native directional brushed metal and its WYSIWYG choice", false,
                    "Brushed metal", "Save");

            // Use one known saved appearance for every QR comparison.
            tapWithScroll("Nacre");
            tap("Save", false);
            waitFor("View card full screen", true, 15000, false);
            probes.put("saved_appearance", readStoredAppearance());
            capture("06-hub-material-front", "Saved nacre card and offline QR controls in the hub", false,
                    "Marta Ruiz", "Show QR", "View card full screen");
            tap("Show QR", false);
            waitFor("Contact card QR code", true, 15000, false);
            capture("07-hub-qr-back", "The actual QR back of the saved native contact card", false,
                    "Back to card", "Contact card QR code");
            tap("Back to card", false);
            waitFor("Marta Ruiz", false, 10000, true);

            tap("View card full screen", true);
            tap("Pause animation", true);
            assertChecked("Pause animation", "Viewer pause did not engage before capture");
            capture("08-viewer-portrait-front", "Portrait full-screen native card and movement controls", false,
                    "Marta Ruiz", "Show QR", "Close card viewer", "Card tilt joystick");
            tap("Show QR", false);
            waitFor("Contact card QR code", true, 15000, false);
            capture("09-viewer-portrait-qr", "Portrait native viewer showing a complete scannable QR", false,
                    "Back to card", "Contact card QR code", "Close card viewer");

            shell("settings put system user_rotation 1");
            waitForLandscape();
            waitFor("Contact card QR code", true, 15000, false);
            assertChecked("Pause animation", "Viewer pause was lost while rotating the QR back");
            probes.put("qr_visible_after_landscape_rotation", true);
            probes.put("pause_preserved_after_landscape_rotation", true);
            capture("10-viewer-landscape-qr", "Landscape QR back with persistent pause and movement feedback", false,
                    "Back to card", "Contact card QR code", "Close card viewer", "Card tilt joystick");
            tap("Back to card", false);
            waitFor("Marta Ruiz", false, 10000, true);
            capture("11-viewer-landscape-front", "Landscape native nacre card with separate controls", false,
                    "Marta Ruiz", "Show QR", "Close card viewer", "Card tilt joystick");
            shell("settings put system user_rotation 0");
            waitForPortrait();
            assertChecked("Pause animation", "Viewer pause was lost while returning to portrait");
            probes.put("pause_preserved_after_return_to_portrait", true);
            tap("Close card viewer", true);
        } catch (Throwable error) {
            failure = error.getClass().getSimpleName() + ": " + error.getMessage();
            try { capture("failure-screen", failure, true); } catch (Throwable ignored) { }
            throw error;
        } finally {
            JSONObject metadata = new JSONObject()
                    .put("app_source_sha", APP_SOURCE)
                    .put("qa_commit_sha", InstrumentationRegistry.getArguments().getString("qaSha", "unspecified"))
                    .put("workflow_run", InstrumentationRegistry.getArguments().getString("workflowRun", "unspecified"))
                    .put("api_level", android.os.Build.VERSION.SDK_INT)
                    .put("device", "Pixel 6 emulator, native 1080x2400 / 420dpi")
                    .put("package", APP)
                    .put("sample_data", "Fictitious Marta Ruiz profile; example.com address")
                    .put("captures", "Raw UiAutomation.takeScreenshot PNGs; no image editing or compositing")
                    .put("physical_nfc_verified", false)
                    .put("scenes", scenes)
                    .put("probes", probes);
            if (failure != null) metadata.put("failure", failure);
            write("capture-manifest.json", metadata.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }

    private void assertChecked(String label, String failure) throws Exception {
        if (!waitFor(label, true, 5000, false).checked) throw new AssertionError(failure);
    }

    private String readStoredAppearance() throws Exception {
        try (FileInputStream profile = new FileInputStream(new File(context.getFilesDir(), "datastore/profile.preferences_pb"))) {
            Class<?> mapClass = Class.forName("androidx.datastore.preferences.PreferencesProto$PreferenceMap");
            Object map = mapClass.getMethod("parseFrom", java.io.InputStream.class).invoke(null, profile);
            Map<?, ?> values = (Map<?, ?>) mapClass.getMethod("getPreferencesMap").invoke(map);
            Object stored = values.get("profile.card.appearance");
            if (stored == null) throw new IllegalStateException("The saved card lost its appearance");
            return (String) stored.getClass().getMethod("getString").invoke(stored);
        }
    }

    private JSONObject diagnoseMasks() throws Exception {
        JSONObject result = new JSONObject();
        try (FileInputStream profile = new FileInputStream(new File(context.getFilesDir(), "datastore/profile.preferences_pb"))) {
            Class<?> mapClass = Class.forName("androidx.datastore.preferences.PreferencesProto$PreferenceMap");
            Object map = mapClass.getMethod("parseFrom", java.io.InputStream.class).invoke(null, profile);
            Map<?, ?> values = (Map<?, ?>) mapClass.getMethod("getPreferencesMap").invoke(map);
            Object stored = values.get("profile.card.appearance");
            String appearance = (String) stored.getClass().getMethod("getString").invoke(stored);
            Class<?> codec = Class.forName("es.igarciadelamo.txoka.domain.profile.CardAppearanceCodec");
            Object decoded = codec.getMethod("decode", String.class).invoke(codec.getField("INSTANCE").get(null), appearance);
            result.put("stored_appearance", appearance).put("decoded_appearance", String.valueOf(decoded));
        }
        Class<?> mosaicClass = Class.forName("es.igarciadelamo.txoka.domain.profile.CardMosaic");
        Class<?> masksClass = Class.forName("es.igarciadelamo.txoka.ui.components.artwork.CardMosaicMasks");
        Object masks = masksClass.getField("INSTANCE").get(null);
        Method mask = masksClass.getMethod("mask", Context.class, mosaicClass, int.class, int.class);
        for (String name : new String[] {"TRIANGLES", "NATURE_FISH"}) {
            JSONObject entry = new JSONObject();
            result.put(name, entry);
            try {
                Object mosaic = mosaicClass.getField(name).get(null);
                int width = name.equals("NATURE_FISH") ? 954 : 256;
                int height = name.equals("NATURE_FISH") ? 600 : 161;
                long started = SystemClock.uptimeMillis();
                Bitmap bitmap = (Bitmap) mask.invoke(masks, context, mosaic, width, height);
                entry.put("present", bitmap != null).put("elapsed_ms", SystemClock.uptimeMillis() - started);
                if (bitmap != null) {
                    int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                    bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    int transparent = 0, opaque = 0, partial = 0;
                    for (int pixel : pixels) {
                        int alpha = pixel >>> 24;
                        if (alpha == 0) transparent++;
                        else if (alpha == 255) opaque++;
                        else partial++;
                    }
                    entry.put("width", bitmap.getWidth()).put("height", bitmap.getHeight())
                            .put("bitmap_config", bitmap.getConfig().name())
                            .put("transparent_pixels", transparent).put("opaque_pixels", opaque).put("partial_pixels", partial);
                } else if (name.equals("NATURE_FISH")) {
                    // The production cache deliberately falls back on parse failure. Obtain its
                    // actual bundled-renderer exception without changing production code.
                    Class<?> rendererClass = Class.forName("es.igarciadelamo.txoka.ui.components.artwork.BundledSvgMask");
                    Object renderer = rendererClass.getField("INSTANCE").get(null);
                    Method render = null;
                    for (Method candidate : rendererClass.getDeclaredMethods()) {
                        Class<?>[] parameters = candidate.getParameterTypes();
                        if (candidate.getName().startsWith("render") && parameters.length == 3
                                && parameters[0] == String.class && parameters[1] == int.class && parameters[2] == int.class) {
                            render = candidate;
                            break;
                        }
                    }
                    if (render == null) throw new NoSuchMethodException("Bundled SVG renderer entrypoint");
                    render.setAccessible(true);
                    String source;
                    try (java.io.InputStream stream = context.getAssets().open("card-artwork/original/nature-fish.svg")) {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
                        source = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
                    }
                    Bitmap direct = (Bitmap) render.invoke(renderer, source, width, height);
                    entry.put("direct_renderer_succeeded", direct != null);
                    if (direct != null) direct.recycle();
                }
            } catch (Throwable error) {
                Throwable cause = error instanceof InvocationTargetException && error.getCause() != null ? error.getCause() : error;
                entry.put("error", cause.getClass().getName() + ": " + cause.getMessage());
                JSONArray stack = new JSONArray();
                for (int i = 0; i < Math.min(12, cause.getStackTrace().length); i++) stack.put(cause.getStackTrace()[i].toString());
                entry.put("stack", stack);
            }
        }
        JSONArray direct = new JSONArray();
        result.put("fish_direct_renderer", direct);
        String svg;
        try (java.io.InputStream input = context.getAssets().open("card-artwork/original/nature-fish.svg")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            svg = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
        for (int[] size : new int[][] {{192, 121}, {954, 600}}) {
            JSONObject item = new JSONObject().put("width", size[0]).put("height", size[1]);
            direct.put(item);
            long started = SystemClock.uptimeMillis();
            try {
                Class<?> rendererClass = Class.forName("es.igarciadelamo.txoka.ui.components.artwork.BundledSvgMask");
                Method render = rendererClass.getDeclaredMethod("render", String.class, int.class, int.class);
                render.setAccessible(true);
                Bitmap bitmap = (Bitmap) render.invoke(rendererClass.getField("INSTANCE").get(null), svg, size[0], size[1]);
                int[] pixels = new int[size[0] * size[1]];
                bitmap.getPixels(pixels, 0, size[0], 0, 0, size[0], size[1]);
                int transparent = 0, nonzero = 0;
                for (int pixel : pixels) {
                    if ((pixel >>> 24) == 0) transparent++; else nonzero++;
                }
                item.put("present", true).put("transparent_pixels", transparent).put("nonzero_alpha_pixels", nonzero);
                bitmap.recycle();
            } catch (Throwable error) {
                Throwable cause = error instanceof InvocationTargetException && error.getCause() != null ? error.getCause() : error;
                item.put("error", cause.getClass().getName() + ": " + cause.getMessage());
                JSONArray stack = new JSONArray();
                for (int i = 0; i < Math.min(12, cause.getStackTrace().length); i++) stack.put(cause.getStackTrace()[i].toString());
                item.put("stack", stack);
            }
            item.put("elapsed_ms", SystemClock.uptimeMillis() - started);
        }
        return result;
    }

    private void tap(String label, boolean description) throws Exception {
        Node node = waitFor(label, description, 15000, false);
        shell("input tap " + node.bounds.centerX() + " " + node.bounds.centerY());
        SystemClock.sleep(800);
    }

    private void tapWithScroll(String label) throws Exception {
        for (int attempt = 0; attempt <= 4; attempt++) {
            Node node = match(snapshot("find-" + label.replace(' ', '-')), label, false, false);
            if (node != null) { shell("input tap " + node.bounds.centerX() + " " + node.bounds.centerY()); SystemClock.sleep(800); return; }
            if (attempt < 4) scrollSettings(true);
        }
        throw new IllegalStateException("Native control not found: " + label);
    }

    private void scrollSettings(boolean upwards) throws Exception {
        updateScreenDimensions();
        int x = screenWidth * 3 / 4;
        int lower = screenHeight * 84 / 100;
        int upper = screenHeight * 63 / 100;
        shell("input swipe " + x + " " + (upwards ? lower : upper) + " " + x + " " + (upwards ? upper : lower) + " 300");
        SystemClock.sleep(600);
    }

    private Node waitFor(String label, boolean description, long timeout, boolean substring) throws Exception {
        long deadline = SystemClock.uptimeMillis() + timeout;
        while (SystemClock.uptimeMillis() < deadline) {
            List<Node> nodes = snapshot("waiting-for-control");
            Node node = match(nodes, label, description, substring);
            if (node != null) return node;
            SystemClock.sleep(400);
        }
        throw new IllegalStateException("Native control did not appear: " + label);
    }

    private List<Node> snapshot(String label) throws Exception {
        List<Node> nodes = new ArrayList<>();
        AccessibilityNodeInfo root = automation.getRootInActiveWindow();
        if (root != null) collect(root, nodes, 0);
        Node launcherError = match(nodes, "Pixel Launcher isn't responding", false, false);
        Node setupError = match(nodes, "com.google.android.googlesdksetup isn't responding", false, false);
        if (launcherError != null || setupError != null) {
            Node close = match(nodes, "Close app", false, false);
            if (close == null) throw new IllegalStateException("Emulator setup ANR could not be dismissed");
            shell("input tap " + close.bounds.centerX() + " " + close.bounds.centerY());
            SystemClock.sleep(1500);
            nodes.clear();
            root = automation.getRootInActiveWindow();
            if (root != null) collect(root, nodes, 0);
        }
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><hierarchy>");
        for (Node node : nodes) xml.append("<node text=\"").append(escape(node.text)).append("\" content-desc=\"")
                .append(escape(node.description)).append("\" package=\"").append(escape(node.packageName))
                .append("\" bounds=\"").append(node.bounds.flattenToString()).append("\" checked=\"")
                .append(node.checked).append("\"/>");
        xml.append("</hierarchy>");
        write(label + ".xml", xml.toString().getBytes(StandardCharsets.UTF_8));
        return nodes;
    }

    private static void collect(AccessibilityNodeInfo source, List<Node> nodes, int depth) {
        if (depth > 60 || nodes.size() > 2500) return;
        Rect rect = new Rect();
        source.getBoundsInScreen(rect);
        nodes.add(new Node(string(source.getText()), string(source.getContentDescription()),
                string(source.getPackageName()), rect, source.isEnabled() && source.isVisibleToUser(), source.isChecked()));
        for (int i = 0; i < source.getChildCount(); i++) {
            AccessibilityNodeInfo child = source.getChild(i);
            if (child != null) collect(child, nodes, depth + 1);
        }
    }

    private static Node match(List<Node> nodes, String label, boolean description, boolean substring) {
        for (Node node : nodes) {
            if (!node.visible || node.bounds.isEmpty()) continue;
            String text = description ? node.description : node.text;
            if (substring && text.contains(label)) return node;
            for (String line : text.split("\\n")) if (line.equals(label)) return node;
        }
        return null;
    }

    private static boolean contains(List<Node> nodes, String label, boolean description, boolean substring) {
        return match(nodes, label, description, substring) != null;
    }

    private static boolean containsApp(List<Node> nodes) {
        for (Node node : nodes) if (node.visible && APP.equals(node.packageName)) return true;
        return false;
    }

    private void capture(String name, String caption, boolean diagnostic, String... required) throws Exception {
        long deadline = SystemClock.uptimeMillis() + 10000;
        List<Node> nodes;
        String missing;
        do {
            nodes = snapshot(name);
            missing = null;
            for (String label : required) {
                if (!contains(nodes, label, false, false) && !contains(nodes, label, true, false)) missing = label;
            }
            if (missing == null) break;
            SystemClock.sleep(300);
        } while (SystemClock.uptimeMillis() < deadline);
        if (missing != null) throw new IllegalStateException("Wrong native scene for " + name + ": " + missing);
        // Let asynchronous optical textures and the flip finish; retain the raw screen PNG.
        SystemClock.sleep(4000);
        Bitmap bitmap = automation.takeScreenshot();
        if (bitmap == null) throw new IllegalStateException("Android screenshot unavailable");
        screenWidth = bitmap.getWidth();
        screenHeight = bitmap.getHeight();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new IllegalStateException("Screenshot encoding failed");
        bitmap.recycle();
        byte[] image = stream.toByteArray();
        write(name + ".png", image);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(image);
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) hex.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        scenes.put(new JSONObject().put("file", name + ".png").put("caption", caption)
                .put("diagnostic", diagnostic).put("width", screenWidth).put("height", screenHeight).put("sha256", hex.toString()));
        System.out.println("Captured native scene " + name + ": " + screenWidth + "x" + screenHeight);
    }

    private void waitForLandscape() throws Exception { waitForOrientation(true); }
    private void waitForPortrait() throws Exception { waitForOrientation(false); }
    private void waitForOrientation(boolean landscape) throws Exception {
        long deadline = SystemClock.uptimeMillis() + 10000;
        do {
            updateScreenDimensions();
            if ((screenWidth > screenHeight) == landscape) { SystemClock.sleep(1500); return; }
            SystemClock.sleep(300);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new IllegalStateException("Device did not rotate");
    }

    private void updateScreenDimensions() {
        Bitmap bitmap = automation.takeScreenshot();
        if (bitmap == null) throw new IllegalStateException("Cannot inspect screen dimensions");
        screenWidth = bitmap.getWidth();
        screenHeight = bitmap.getHeight();
        bitmap.recycle();
    }

    private byte[] shell(String command) throws Exception {
        try (ParcelFileDescriptor.AutoCloseInputStream stream =
                     new ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return bytes.toByteArray();
        }
    }

    private void write(String name, byte[] bytes) throws Exception {
        try (FileOutputStream stream = new FileOutputStream(new File(output, name))) { stream.write(bytes); }
    }

    private static String string(CharSequence value) { return value == null ? "" : value.toString(); }
    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("\n", "&#10;").replace("\r", "&#13;");
    }

    private static final class Node {
        final String text;
        final String description;
        final String packageName;
        final Rect bounds;
        final boolean visible;
        final boolean checked;
        Node(String text, String description, String packageName, Rect bounds, boolean visible, boolean checked) {
            this.text = text; this.description = description; this.packageName = packageName;
            this.bounds = bounds; this.visible = visible; this.checked = checked;
        }
    }
}
