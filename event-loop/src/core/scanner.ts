import fs from "fs/promises";
import { concatPaths, createPath, type Path } from "./path.js";
import { FsStatReport } from "./report.js";

export interface FsStatScanner {
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
    try {
      const exploreFile = (fileName: string) =>
        this.traverseFile(concatPaths(path, createPath(fileName)), report);
      const exploreDir = (dirName: string) =>
        this.traverseDirectory(concatPaths(path, createPath(dirName)), report);
      const entries = await fs.readdir(path, {
        withFileTypes: true,
      });
      const directories = entries.filter((entry) => entry.isDirectory());
      const files = entries.filter((entry) => entry.isFile());
      await Promise.all([
        ...files.map((file) => exploreFile(file.name)),
        ...directories.map((dir) => exploreDir(dir.name)),
      ]);
    } catch (err) {
      if (this.isAccessError(err)) {
        return;
      }
      throw this.parseScanningError(err, path);
    }
  }

  private async traverseFile(
    filePath: Path,
    report: FsStatReport,
  ): Promise<void> {
    try {
      const stats = await fs.stat(filePath);
      report.updateReport(stats.size);
    } catch (err) {
      if (this.isAccessError(err)) {
        return;
      }
      throw this.parseScanningError(err, filePath);
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
