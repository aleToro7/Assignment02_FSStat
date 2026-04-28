import { normalize as normalizePath, join as joinPath, sep } from "node:path";

/**
 * Branded Type for a validated and normalized File System Path.
 */
export type Path = string & { readonly __brand: unique symbol };

/**
 * Creates a Path branded type from a string input.
 * It normalizes the path and ensures it is in a consistent format.
 * @param input The raw path string.
 * @returns A validated Path branded type.
 * @throws {Error} if the input is not a valid path (e.g., empty string).
 */
export function createPath(input: string): Path {
  if (!input) {
    throw new Error("Invalid path: input cannot be empty :(");
  }
  const normalized = normalizePath(input);
  return normalized.length > 1 && normalized.endsWith(sep)
    ? (normalized.slice(0, -1) as Path)
    : (normalized as Path);
}

/**
 * Concatenates two Path branded types using the OS-specific separator.
 *
 * @param base The base Path.
 * @param sub The sub-path to append.
 * @returns A new Path representing the concatenation.
 */
export function concatPaths(base: Path, sub: Path): Path {
  return joinPath(base, sub) as Path;
}
