import axios from "axios";
import { extractFilename, triggerBrowserDownload } from "./taskExportService";

const EXPORT_URL = "/api/tasks/export/zip";

/**
 * Axios equivalent of downloadTaskExportZip(), for projects that
 * standardize on axios instead of fetch.
 */
export async function downloadTaskExportZipAxios(): Promise<void> {
    const response = await axios.get(EXPORT_URL, {
        responseType: "blob",
        withCredentials: true,
        headers: { Accept: "application/zip" },
        validateStatus: (status) => status === 200,
    });

    const filename = extractFilename(
        (response.headers["content-disposition"] as string) ?? null
    );
    triggerBrowserDownload(response.data as Blob, filename);
}
