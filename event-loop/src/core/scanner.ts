import { readdir } from "fs/promises";

export interface FsStatScanner {
  /**
   * Given a path, it returns the number of directories contained in that path and all its subdirectories.
   * @param path the path to scan
   * @returns a report containing the number of directories contained in the given path and all its subdirectories
   * @throws {Error} if the given path does not exist or is not a directory
   */
  getSubdirectories(path: string): Promise<string[]>;
}

class FsStatScannerImpl implements FsStatScanner {
  async getSubdirectories(path: string): Promise<string[]> {
    try {
      const files = await readdir(path, { withFileTypes: true });
      const directories = files
        .filter((file) => file.isDirectory())
        .map((dir) => dir.name);
      return directories;
    } catch (err) {
      throw new Error(`Error while scanning path ${path}: ${err}`);
    }
  }
}

export function FsStatScanner(): FsStatScanner {
  return new FsStatScannerImpl();
}
