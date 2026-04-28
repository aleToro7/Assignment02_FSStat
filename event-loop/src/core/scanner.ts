import type { FsStatReport } from "#types";
import { readdir } from "fs/promises";
import { concatPaths, createPath, type Path } from "./path.js";

export interface FsStatScanner {
  /**
   * Counts the number of directories contained in the given path and all its subdirectories.
   *
   * **Note**: Any directory that cannot be accessed due to permissions issues will be silently ignored and not counted.
   * However, if the initial path provided is inaccessible, an error will be thrown.
   *
   * **Note**: The following directories are blacklisted and will be ignored during the scanning process:
   * - /proc
   * - /sys
   * - /dev
   * - /run
   * - /var/run
   * - /Volumes
   * - /Network
   *
   * @param path the path to scan
   * @returns a report containing the number of directories contained in the given path and all its subdirectories
   * @throws {Error} if the given path does not exist or is not a directory
   */
  countSubdirectories(path: string): Promise<FsStatReport>;
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

  async countSubdirectories(path: string): Promise<FsStatReport> {
    let toVisit: Path[] = [createPath(path)];
    let count = 0;
    while (toVisit.length > 0) {
      const subdirectories = await Promise.all(
        toVisit.map((currentPath) => this.getSubdirectories(currentPath)),
      ).then((res) => res.flat());
      count += subdirectories.length;
      toVisit = [...subdirectories];
    }

    return { numOfDirs: count };
  }

  /**
   * Given a path, it returns the number of directories contained in that path and all its subdirectories.
   * @param path the path to scan
   * @returns a report containing the number of directories contained in the given path and all its subdirectories
   * @throws {Error} if the given path does not exist or is not a directory
   */
  private async getSubdirectories(path: string): Promise<Path[]> {
    if (this.isBlacklisted(path)) return [];
    const basePath = createPath(path);
    try {
      const files = await readdir(basePath, {
        withFileTypes: true,
      });
      const directories = files
        .filter((file) => file.isDirectory())
        .map((dir) => concatPaths(basePath, createPath(dir.name)));
      return directories;
    } catch (err) {
      if (this.isAccessError(err)) {
        return [];
      }
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
