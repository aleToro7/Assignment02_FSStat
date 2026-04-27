interface Polite {
  name: string;
  greet(other: Polite): void;
}

class PersonImpl implements Polite {
  constructor(public name: string) {}

  greet(other: Polite): void {
    console.log(`Hello, ${other.name}! My name is ${this.name}.`);
  }
}

function Person(name: string): Polite {
  return new PersonImpl(name);
}

const astro = Person("Astro");
const nax = Person("Nax");

astro.greet(nax);
nax.greet(astro);
