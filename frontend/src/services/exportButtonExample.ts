// Example usage inside a React/Vue/etc. component (adapt to your framework).
// Not a runnable file on its own — shows the intended call pattern.

import { downloadTaskExportZip } from "./taskExportService";

async function handleExportClick(
    setExporting: (v: boolean) => void,
    showToast: (msg: string) => void
) {
    try {
        setExporting(true);
        await downloadTaskExportZip();
    } catch (err) {
        console.error(err);
        showToast("Failed to export tasks. Please try again.");
    } finally {
        setExporting(false);
    }
}

export { handleExportClick };
