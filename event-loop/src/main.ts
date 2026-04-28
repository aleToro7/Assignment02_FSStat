import { FsStatScanner } from "#core/scanner";
import { benchmark } from "#utils/benchmark";

const dirPath = process.argv[2] ?? "/home/diottanax/";

const {
  duration,
  result: { numOfDirs },
} = await benchmark(() => FsStatScanner().countSubdirectories(dirPath));

console.log(`Number of subdirectories in ${dirPath}: ${numOfDirs}`);
console.log(`Time taken: ${duration.toFixed(2)} ms`);
