const EXPORT_URL = "/api/tasks/export/zip";

/**
 * Extracts the filename from a Content-Disposition header, falling back
 * to a timestamped default if the header is missing or unparsable.
 */
function extractFilename(contentDisposition: string | null): string {
    const fallback = `tasks-export-${new Date().toISOString().slice(0, 10)}.zip`;
    if (!contentDisposition) return fallback;

    const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match?.[1]) return decodeURIComponent(utf8Match[1]);

    const basicMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
    return basicMatch?.[1] ?? fallback;
}

/**
 * Requests the task export ZIP and triggers a browser download.
 * Throws on non-2xx responses so callers can surface an error to the UI.
 */
export async function downloadTaskExportZip(): Promise<void> {
    const response = await fetch(EXPORT_URL, {
        method: "GET",
        headers: {
            Accept: "application/zip",
            // Add Authorization header here if using bearer tokens instead of cookies:
            // Authorization: `Bearer ${getAccessToken()}`,
        },
        credentials: "include", // send session cookie if using cookie-based auth
    });

    if (!response.ok) {
        const message = await safeReadErrorBody(response);
        throw new Error(`Export failed (${response.status}): ${message}`);
    }

    const blob = await response.blob();
    const filename = extractFilename(response.headers.get("Content-Disposition"));

    triggerBrowserDownload(blob, filename);
}

export function triggerBrowserDownload(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    // Revoke asynchronously — some browsers need the click to fully process first.
    setTimeout(() => window.URL.revokeObjectURL(url), 1000);
}

async function safeReadErrorBody(response: Response): Promise<string> {
    try {
        const text = await response.text();
        return text || response.statusText;
    } catch {
        return response.statusText;
    }
}

export { extractFilename };
