import fs from "fs/promises";
import { concatPaths, createPath, type Path } from "./path.js";
import { FsStatReport } from "./report.js";

/**
 * FsStatScanner defines the interface for scanning a directory and generating a report on the number of files and their size distribution.
 */
export interface FsStatScanner {
  /**
   * Generates a report for the given directory path by computing statistics about:
   * - The total number of files contained in the given path and all its subdirectories.
   * - An array where the i-th element represents the number of files whose size falls into the i-th band.
   * The bands are defined by dividing the range of file sizes (from 0 to maxSize) into equal intervals.
   * @param path The path of the directory to scan.
   * @param maxSize The maximum file size in each band.
   * @param bands The number of size bands to divide the file sizes into.
   */
  getReport(
    path: string,
    maxSize: number,
    bands: number,
  ): Promise<FsStatReport>;
}

class FsStatScannerImpl implements FsStatScanner {
  private readonly blacklist = new Set([
    "/proc",
    "/sys",
    "/dev",
    "/run",
    "/var/run",
    "/Volumes",
    "/Network",
  ]);

  async getReport(
    path: string,
    maxSize: number,
    bands: number,
  ): Promise<FsStatReport> {
    const report = FsStatReport(maxSize, bands);
    await this.traverseDirectory(createPath(path), report);
    return report;
  }

  private async traverseDirectory(
    path: Path,
    report: FsStatReport,
  ): Promise<void> {
    if (this.isBlacklisted(path)) return;
    const exploreFile = (fileName: string) =>
      this.traverseFile(concatPaths(path, createPath(fileName)), report);
    const exploreDir = (dirName: string) =>
      this.traverseDirectory(concatPaths(path, createPath(dirName)), report);
    const entries = await fs
      .readdir(path, {
        withFileTypes: true,
      })
      .catch((err) => {
        this.ignoreAccessError(err, path);
        return [];
      });
    const directories = entries.filter((entry) => entry.isDirectory());
    const files = entries.filter((entry) => entry.isFile());
    await Promise.all([
      ...files.map((file) => exploreFile(file.name)),
      ...directories.map((dir) => exploreDir(dir.name)),
    ]);
  }

  private async traverseFile(
    filePath: Path,
    report: FsStatReport,
  ): Promise<void> {
    fs.stat(filePath)
      .then((stats) => report.updateReport(stats.size))
      .catch((err) => this.ignoreAccessError(err, filePath));
  }

  private async ignoreAccessError(err: unknown, path: Path): Promise<void> {
    if (!this.isAccessError(err)) {
      throw this.parseScanningError(err, path);
    }
  }

  private isBlacklisted(path: string): boolean {
    return this.blacklist.has(path);
  }

  private isAccessError(err: unknown): boolean {
    return (
      err instanceof Error &&
      "code" in err &&
      (err.code === "EACCES" || err.code === "EPERM")
    );
  }

  private parseScanningError(err: unknown, path: string): Error {
    if (err instanceof Error && "code" in err) {
      switch (err.code) {
        case "ENOENT":
          throw new Error(`Path ${path} does not exist :(`);
        case "ENOTDIR":
          throw new Error(`Path ${path} is not a directory :(`);
      }
    }
    throw new Error(`Error while scanning path ${path}: ${err}`);
  }
}

export function FsStatScanner(): FsStatScanner {
  return new FsStatScannerImpl();
}
