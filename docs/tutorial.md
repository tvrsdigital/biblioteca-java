# Tutorial – Sistema de Biblioteca em Java (POO)

## Visão geral

O projeto é um sistema de controle de biblioteca em Java que aplica herança, polimorfismo, associação e agregação, com persistência em SQLite por uma camada DAO e interface Swing. Ele controla leitores, funcionários, obras, cópias, empréstimos e reservas.

| Item | Escolha |
| --- | --- |
| Linguagem | Java 25 (LTS) |
| Build e dependências | Maven |
| Banco de dados | SQLite, via driver JDBC `sqlite-jdbc` |
| Interface gráfica | Swing |
| Versionamento | Git + GitHub (repositório `biblioteca-java`) |
| Editor | VSCode com Extension Pack for Java |

**Arquitetura em camadas.** Cada camada tem uma responsabilidade e só conversa com a camada logo abaixo:

| Pacote | Responsabilidade |
| --- | --- |
| `view` | Telas Swing. Captura as exceções e mostra mensagens ao usuário |
| `service` | Regras de negócio (limite de empréstimos, atraso, multa, fila de reservas) |
| `dao` | Acesso ao banco: traduz objetos em linhas de tabela e vice-versa |
| `model` | Classes do domínio (Pessoa, Leitor, Obra, Cópia…). Só guardam dados e comportamento próprio |
| `exception` | Exceções personalizadas do sistema |

&#91;embedded content: arquitetura em camadas · chamadas descem, exceções sobem\]

As chamadas descem da tela até o banco; quando algo dá errado, a exceção sobe pelo mesmo caminho e só é tratada na tela. O `model` circula entre todas as camadas.

**Como usar este documento.** As partes 1 a 6 seguem a ordem em que o projeto foi construído. As seções de teoria explicam cada conceito de POO com o exemplo real do código. As últimas seções (decisões, erros comuns e perguntas prováveis) servem de revisão antes da apresentação.

## Parte 1 – Ambiente e ferramentas (passos 1 a 5)

**Passo 1 – Instalação.** Foram instalados o JDK 25 (Eclipse Temurin), o Maven, o Git e a extensão Extension Pack for Java no VSCode. Para conferir:

```bash
java -version
javac -version
mvn -version
git --version
```

O `java` (máquina virtual) e o `javac` (compilador) precisam estar na mesma versão, e a saída do `mvn -version` deve mostrar que o Maven usa o JDK 25.

**Passo 2 – Criação do projeto Maven.**

```bash
mvn archetype:generate -DgroupId=br.com.biblioteca -DartifactId=biblioteca -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.5 -DinteractiveMode=false
```

- `groupId` identifica o dono do projeto e vira o pacote base (`br.com.biblioteca`).
- `artifactId` é o nome do projeto e da pasta.
- O Maven separa o código em `src/main/java` (sistema) e `src/test/java` (testes); `target/` guarda o código compilado e é gerado automaticamente.

**Passo 3 – Ajustes no `pom.xml`.** O `pom.xml` é a “receita” do projeto:

- `maven.compiler.release` = 25: gera código para o Java 25.
- Dependência `org.xerial:sqlite-jdbc`: o driver JDBC do SQLite. O Maven baixa sozinho.
- Plugin `exec-maven-plugin` com `mainClass` = `br.com.biblioteca.App`: permite rodar o programa.
- `<pluginManagement>` apenas fixa versões; `<plugins>` ativa o plugin no projeto.

Comando para compilar e executar, usado durante todo o projeto:

```bash
mvn compile exec:java
```

**JDBC** é a API padrão do Java para bancos relacionais: o Java define as interfaces (`Connection`, `PreparedStatement`, `ResultSet`) e cada banco fornece um driver que as implementa. Trocar de banco é, em grande parte, trocar o driver.

**Passo 4 – Git local.** O `.gitignore` lista o que não é versionado: o que pode ser gerado (`target/`, `*.db`) e arquivos de IDE (`.vscode/`). O Git trabalha em três áreas:

```
arquivos modificados  --git add-->  staging area  --git commit-->  histórico
```

```bash
git init -b main
git add .
git commit -m "chore: estrutura inicial do projeto Maven"
```

As mensagens seguem o padrão **Conventional Commits**: `feat:` (funcionalidade), `fix:` (correção), `docs:` (documentação), `chore:` (configuração).

**Passo 5 – GitHub.** O repositório remoto foi criado vazio (sem README nem .gitignore, para não conflitar com o histórico local) e conectado:

```bash
git remote add origin https://github.com/<usuario>/biblioteca-java.git
git push -u origin main
```

`remote` é um repositório em outro lugar; `origin` é o apelido padrão; `push` envia os commits; `-u` liga a branch local à remota, para os próximos `git push` funcionarem sem argumentos.

## Parte 2 – Requisitos e modelagem (passos 6 e 7)

Antes de escrever classes, foram definidos o que o sistema faz e quais regras ele obedece. Isso está em `docs/requisitos.md` e no diagrama de classes em `docs/diagrama-classes.md`.

**A distinção central: Obra × Cópia.** A Obra é o título abstrato (“Dom Casmurro”, com autor e ISBN). A Cópia é o exemplar físico na prateleira; uma obra pode ter várias. Daqui saem duas regras de modelagem:

- **o empréstimo é de uma Cópia**, porque o leitor leva um exemplar físico específico, real e finito;
- **a reserva é de uma Obra**, porque o leitor quer qualquer cópia daquele título que ficar livre.

**Regras de negócio**

| Código | Regra |
| --- | --- |
| RN01 | Aluno pode ter até 3 empréstimos ativos, com prazo de 7 dias |
| RN02 | Professor pode ter até 5 empréstimos ativos, com prazo de 14 dias |
| RN03 | Só pode ser emprestada uma cópia com situação DISPONÍVEL |
| RN04 | Leitor com empréstimo em atraso não pode fazer novo empréstimo |
| RN05 | Só é possível reservar uma obra que não tenha cópia disponível |
| RN06 | Ao devolver uma cópia de obra com reserva ativa, a cópia fica RESERVADA para o leitor da reserva mais antiga |
| RN07 | Cada dia de atraso na devolução gera multa de R$ 1,00 |
| RN08 | Só funcionários autenticados (login e senha) acessam o sistema |
| RN09 | O empréstimo é registrado em nome do funcionário logado |

**Diagrama de classes.** Foi escrito em Mermaid, um formato de texto que o GitHub desenha automaticamente. Os símbolos usados:

| Símbolo | Conceito | Exemplo no projeto |
| --- | --- | --- |
| `<\|--` | Herança | Pessoa → Leitor; Leitor → Aluno |
| método com `*` | Método abstrato | `getPrazoDias()` em Leitor |
| `o--` | Agregação | Obra ◇— Cópia |
| `-->` | Associação | Empréstimo → Leitor |
| `$` | Membro estático | `PRAZO_DIAS` |

O diagrama evoluiu junto com o código (por exemplo, `id` passou de `int` para `Integer` e surgiu o `getTipo()`). Isso é normal: o desenho inicial é um ponto de partida, não um contrato.

## Teoria – Os quatro pilares da POO

Os quatro pilares aparecem no projeto em pontos bem definidos: abstração em `Pessoa` e `Leitor`, encapsulamento em todos os atributos `private`, herança na hierarquia de pessoas e polimorfismo no prazo e no limite dos leitores.

### 1. Abstração

Abstrair é representar só o que importa para o problema e esconder detalhes. Em Java, uma **classe abstrata** (`abstract`) representa um conceito geral que nunca existe sozinho: não se faz `new` dela.

- `Pessoa` é abstrata: no sistema, toda pessoa é um Leitor ou um Funcionário.
- `Leitor` é abstrata: todo leitor é um Aluno ou um Professor.
- Um **método abstrato** declara só a assinatura, terminando em `;`, sem corpo. Ele é um contrato: “todo leitor sabe informar o prazo”, sem dizer qual.

```java
public abstract int getPrazoDias();   // sem chaves, sem corpo
```

|  | Classe abstrata (`Leitor`) | Classe concreta (`Aluno`) |
| --- | --- | --- |
| Pode fazer `new`? | Não | Sim |
| Método abstrato | Declara a assinatura (`;`) | Implementa com `@Override` e corpo |
| Papel | Define o contrato | Cumpre o contrato |

### 2. Encapsulamento

Encapsular é proteger o estado do objeto: os atributos são `private` e só são acessados por métodos. Assim, a classe controla como seus dados mudam.

| Modificador | Quem acessa |
| --- | --- |
| `private` | só a própria classe |
| (nenhum) | classes do mesmo pacote |
| `protected` | mesmo pacote + subclasses |
| `public` | todo mundo |

Exemplos fortes de encapsulamento no projeto:

- **Obra e suas cópias:** `getCopias()` devolve `Collections.unmodifiableList(copias)`, uma visão somente leitura, e não existe `setCopias`. A única forma de incluir uma cópia é `adicionarCopia()`, que também faz `copia.setObra(this)`. Tentar `obra.getCopias().add(...)` lança `UnsupportedOperationException`.
- **Empréstimo e Reserva quase sem setters:** a data prevista e a multa só mudam por métodos com significado (`registrarDevolucao`), e a situação da reserva só por `atender()` e `cancelar()`.
- **Reserva protegendo o próprio estado:** `atender()` lança `IllegalStateException` se a reserva não estiver ativa.

### 3. Herança

Herança (`extends`) expressa a relação **“é um”**: um Aluno é um Leitor, que é uma Pessoa. A subclasse herda atributos e métodos e pode acrescentar os seus.

```
Pessoa (abstrata: id, nome, cpf, email, telefone)
├── Leitor (abstrata: matricula)
│   ├── Aluno
│   └── Professor
└── Funcionario (cargo, login, senhaHash, salt)
```

- `super(...)` chama o construtor da classe-mãe e precisa ser a **primeira linha** do construtor.
- Nome, CPF e e-mail são escritos uma vez só, em `Pessoa`.
- O `Funcionario` herda o `toString()` de `Pessoa` sem reescrevê-lo; por isso imprime o nome.

### 4. Polimorfismo

Polimorfismo é a capacidade de objetos de tipos diferentes responderem à **mesma chamada** de formas diferentes. O Java escolhe a versão do método **em tempo de execução**, pelo tipo real do objeto.

```java
// Aluno: PRAZO_DIAS = 7   |   Professor: PRAZO_DIAS = 14
for (Leitor l : leitores) {
    System.out.println(l + ": prazo " + l.getPrazoDias());
}
// Ana (ALUNO): prazo 7
// Carlos (PROFESSOR): prazo 14
```

O laço trata todos como `Leitor` e nunca pergunta o tipo. Onde isso é usado no projeto:

- **No construtor do Empréstimo:** `dataPrevista = dataEmprestimo.plusDays(leitor.getPrazoDias())`, sem nenhum `if`.
- **No limite de empréstimos:** `getLimiteEmprestimos()` devolve 3 ou 5.
- **No DAO:** `leitor.getTipo()` grava “ALUNO” ou “PROFESSOR” sem `instanceof`.

**Por que isso é melhor que `if (leitor instanceof Aluno)`?** Com `if`, cada novo tipo de leitor obriga a alterar todos os lugares com essa verificação. Com polimorfismo, basta criar uma nova subclasse. Isso é o **princípio aberto/fechado**: aberto para extensão, fechado para modificação.

**Sobrecarga × sobrescrita** (cai muito em prova):

|  | Sobrecarga (overloading) | Sobrescrita (overriding) |
| --- | --- | --- |
| Onde | Na mesma classe | Na subclasse |
| Assinatura | Parâmetros diferentes | Parâmetros iguais |
| No projeto | Os dois construtores de cada classe | `toString()`, `getPrazoDias()` |
| Decidida em | Tempo de compilação | Tempo de execução |

A anotação `@Override` pede ao compilador para conferir que o método realmente sobrescreve outro. Se o nome estiver errado (como `getLimiteEmprestimo` sem o “s”), ele acusa o erro.

## Teoria – Relacionamentos entre classes

O projeto usa os quatro relacionamentos pedidos no enunciado: herança (Pessoa → Leitor → Aluno), agregação (Obra ◇— Cópia) e associação (Empréstimo e Reserva ligando Leitor, Cópia, Obra e Funcionário). A composição entra como contraste, para explicar por que Obra–Cópia é agregação.

| Relacionamento | Significado | Força do vínculo | No projeto |
| --- | --- | --- | --- |
| Herança | “é um” | Classe-filha estende a mãe | Aluno é um Leitor; Funcionário é uma Pessoa |
| Associação | “conhece / usa” | Fraco: cada um existe sozinho | Empréstimo → Leitor, Cópia e Funcionário; Reserva → Leitor e Obra |
| Agregação | “tem / é formado por” | Médio: a parte existe independente do todo | Obra tem uma lista de Cópias |
| Composição | “é parte de” | Forte: a parte não existe sem o todo | (não usada; ver abaixo) |

### Associação

Um objeto guarda uma referência a outro e o usa, mas os dois têm vida própria. O Empréstimo aponta para um Leitor, uma Cópia e um Funcionário; se o empréstimo for apagado, o leitor continua existindo. No banco, cada associação vira uma **chave estrangeira** (`leitor_id`, `copia_id`, `funcionario_id`).

### Agregação

É uma associação do tipo todo–parte: a Obra é o todo e as Cópias são as partes. A Cópia é criada fora da Obra (`new Copia("C001")`) e depois agregada com `obra.adicionarCopia(copia)`; o DAO também carrega as cópias separadamente.

A relação é **bidirecional**: a Obra tem a lista de Cópias e cada Cópia conhece sua Obra. Por isso `adicionarCopia()` atualiza os dois lados:

```java
public void adicionarCopia(Copia copia) {
    copias.add(copia);     // lado da Obra
    copia.setObra(this);   // lado da Cópia: this = esta obra
}
```

### Por que não composição?

Na composição, o todo cria e destrói as partes, e elas não fazem sentido fora dele (exemplo clássico: os cômodos de uma casa). Conceitualmente, uma cópia sem obra também seria estranha, mas no código a Cópia é um objeto independente, criado fora e agregado depois. Por isso o diagrama usa o losango vazio da agregação (◇).

### Herança × associação

Uma dúvida comum: por que Empréstimo não herda de Leitor? Porque um empréstimo **não é um** leitor; ele **tem** um leitor. A pergunta “é um” ou “tem um” decide entre herança e associação.

## Parte 3 – Implementação do model (passos 8 a 13)

O pacote `model` tem 11 tipos: 7 classes, 2 classes abstratas e 2 enums. Todos seguem o mesmo padrão: atributos `private`, dois construtores e getters.

### Sintaxe base de uma classe

```java
package br.com.exemplo;                     // pacote = pasta; sempre a 1ª linha

public abstract class Veiculo {             // abstract: não se faz new Veiculo()
    private Integer id;                     // atributo: modificador + tipo + nome
    private String placa;

    protected Veiculo(String placa) {       // construtor: mesmo nome, SEM tipo de retorno
        this.placa = placa;                 // this.placa = atributo; placa = parâmetro
    }

    public String getPlaca() { return placa; }              // getter
    public void setPlaca(String placa) { this.placa = placa; } // setter

    @Override
    public String toString() { return placa; }  // sobrescreve o método de Object
}
```

Convenções de nome: `Classe` (PascalCase), `atributo` e `metodo()` (camelCase), `CONSTANTE` (maiúsculas), `pacote` (minúsculas). Getter de `boolean` usa `is` (`isDisponivel()`). O arquivo deve ter exatamente o nome da classe pública.

### O padrão dos dois construtores

Cada entidade tem um construtor para **objeto novo** (sem `id`, que fica `null`) e outro para **objeto lido do banco** (com `id`). O primeiro chama o segundo com `this(...)`, encadeamento de construtores, para as atribuições ficarem num lugar só:

```java
protected Pessoa(String nome, String cpf, String email, String telefone) {
    this(null, nome, cpf, email, telefone);
}
protected Pessoa(Integer id, String nome, String cpf, String email, String telefone) {
    this.id = id;
    // ... demais atribuições
}
```

### As classes

| Classe | Tipo | Destaques |
| --- | --- | --- |
| `Pessoa` | Abstrata | `id` como `Integer`; construtores `protected`; `toString()` devolve o nome |
| `Leitor` | Abstrata, estende Pessoa | `matricula`; métodos abstratos `getPrazoDias()`, `getLimiteEmprestimos()`, `getTipo()` |
| `Aluno` | Concreta, estende Leitor | Constantes `PRAZO_DIAS = 7`, `LIMITE_EMPRESTIMOS = 3`; `getTipo()` = "ALUNO" |
| `Professor` | Concreta, estende Leitor | Constantes 14 e 5; `getTipo()` = "PROFESSOR" |
| `Funcionario` | Concreta, estende Pessoa | `cargo`, `login`, `senhaHash`, `salt` (nunca a senha em texto puro) |
| `SituacaoCopia` | Enum | DISPONIVEL, EMPRESTADA, RESERVADA, MANUTENCAO |
| `SituacaoReserva` | Enum | ATIVA, ATENDIDA, CANCELADA |
| `Copia` | Concreta | Começa DISPONÍVEL; conhece sua Obra; `isDisponivel()` |
| `Obra` | Concreta | Lista `final` de cópias; `adicionarCopia()`; `temCopiaDisponivel()` |
| `Emprestimo` | Concreta | Prazo polimórfico; multa em `BigDecimal`; quase sem setters |
| `Reserva` | Concreta | Começa ATIVA; `atender()` e `cancelar()` com proteção de estado |

### Detalhes que valem estudar

- **Constantes** (`private static final int PRAZO_DIAS = 7;`): `static` pertence à classe, não ao objeto; `final` não muda. Evitam “números mágicos” soltos no código.
- **Enums:** tipo com conjunto fixo de valores. Compara-se com `==`; converte-se com `name()` (enum → texto) e `valueOf()` (texto → enum).
- **Lista nascida na declaração:** `private final List<Copia> copias = new ArrayList<>();` evita `NullPointerException`; o `final` impede trocar a lista inteira, mas permite mudar o conteúdo.
- **Métodos que reaproveitam outros:** `temCopiaDisponivel()` usa `isDisponivel()`; `estaAtrasado()` usa `isDevolvido()`; `registrarDevolucao()` usa `calcularMulta()`.
- **Retorno direto de comparação:** em vez de `if (x) return true; return false;`, escreve-se `return situacao == SituacaoReserva.ATIVA;`.
- **Guard clause:** tratar o caso especial primeiro e sair cedo (`if (diasAtraso <= 0) return BigDecimal.ZERO;`).
- **Datas como parâmetro:** `estaAtrasado(LocalDate hoje)` e `calcularMulta(LocalDate data)` recebem a data em vez de chamar `LocalDate.now()` por dentro. Isso permite testar um atraso de 10 dias sem esperar 10 dias.

### APIs do Java usadas

- **`LocalDate`**: data sem horário. `plusDays(7)`, `isAfter(outra)`, `LocalDate.of(2026, 11, 21)`, `toString()` e `parse()` no formato ISO.
- **`ChronoUnit.DAYS.between(a, b)`**: dias de `a` até `b` (negativo se `b` vier antes).
- **`BigDecimal`**: valores monetários exatos. Criado a partir de `String` (`new BigDecimal("1.00")`), nunca de `double`; operações são métodos (`multiply`), e o objeto é imutável.
- **`Collections.unmodifiableList`**: visão somente leitura de uma lista.

## Parte 4 – Banco de dados e camada DAO (passos 14 a 19)

A persistência usa SQLite por JDBC, com uma fábrica de conexões, um inicializador de tabelas e um DAO por entidade. Nenhuma outra camada escreve SQL.

### Passo 14 – ConexaoFactory

- URL `jdbc:sqlite:biblioteca.db`: o arquivo é criado sozinho na raiz do projeto.
- **Padrão Factory:** todos pedem conexão à mesma classe. Trocar de banco muda só ela.
- Classe `final` com construtor `private` e método `static`: é um utilitário, não se instancia.
- `PRAGMA foreign_keys = ON`: o SQLite não verifica chaves estrangeiras por padrão; precisa ser ligado a cada conexão.

### Passo 15 – Tabelas

**Mapeamento da herança.** Bancos relacionais não têm herança, então foram usadas duas estratégias:

- **Tabela por classe concreta:** `Pessoa` não tem tabela; seus campos aparecem em `leitor` e `funcionario`.
- **Tabela única:** `Aluno` e `Professor` dividem a tabela `leitor`, com a coluna `tipo` preenchida por `getTipo()`.

**Tradução de tipos:**

| Java | SQLite | Conversão |
| --- | --- | --- |
| `Integer id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | O banco gera o id |
| `String` | `TEXT` | Direta |
| `LocalDate` | `TEXT` ("2026-11-21") | `toString()` / `LocalDate.parse()` |
| `BigDecimal` | `TEXT` ("3.00") | `toPlainString()` / `new BigDecimal(texto)` |
| enum | `TEXT` ("DISPONIVEL") | `name()` / `valueOf()` |
| associação | `INTEGER REFERENCES tabela(id)` | Grava-se só o id do outro objeto |

**Restrições:** `NOT NULL`, `UNIQUE` (CPF, matrícula, login, ISBN, código da cópia), `CHECK` (tipo de leitor) e chaves estrangeiras. Elas são uma segunda linha de defesa: mesmo que o Java falhe, o banco recusa dados inválidos. `CREATE TABLE IF NOT EXISTS` permite rodar o inicializador a cada execução.

### SQL mínimo usado no projeto

| Comando | Para quê | Forma |
| --- | --- | --- |
| `INSERT` | Inserir uma linha | `INSERT INTO tabela (colunas) VALUES (?, ?)` |
| `SELECT` | Consultar | `SELECT * FROM tabela WHERE coluna = ? ORDER BY nome` |
| `UPDATE` | Alterar | `UPDATE tabela SET coluna = ? WHERE id = ?` |
| `DELETE` | Apagar | `DELETE FROM tabela WHERE id = ?` |

`UPDATE` e `DELETE` sem `WHERE` afetam a tabela inteira. Nulo se testa com `IS NULL` (um empréstimo ativo tem `data_devolucao IS NULL`). `RETURNING id` faz o `INSERT` devolver o id gerado.

### Passos 16 a 19 – Os DAOs

**DAO (Data Access Object)** é a classe que traduz objetos em linhas de tabela e vice-versa. Todo método segue o mesmo esqueleto:

1. escrever o SQL numa `String` (text block `"""` para várias linhas);
2. abrir `Connection` e `PreparedStatement` num try-with-resources;
3. preencher cada `?` com `setString`, `setInt`… (a contagem começa em 1);
4. executar: `executeQuery` se devolve linhas, `executeUpdate` se só altera;
5. ler o `ResultSet` com `rs.next()` e `rs.getString("coluna")`.

**`PreparedStatement` e SQL Injection.** Nunca se concatena texto do usuário no SQL. Com `?`, o valor é sempre tratado como dado, nunca como comando; um texto malicioso como `' OR '1'='1` não muda a consulta.

**Try-with-resources.** Conexão, comando e resultado são fechados automaticamente ao sair do bloco, mesmo se ocorrer erro.

| DAO | Métodos | Destaque |
| --- | --- | --- |
| `LeitorDAO` | salvar, atualizar, excluir, buscarPorId, listarTodos | `montarLeitor` com `switch` no tipo: único ponto que escolhe Aluno ou Professor |
| `FuncionarioDAO` | CRUD + `buscarPorLogin` | Usado na autenticação |
| `ObraDAO` | CRUD | Ao ler uma obra, carrega as cópias junto |
| `CopiaDAO` | salvar, atualizar, excluir, `carregarCopias`, buscarPorId | Reconstrói a agregação pela porta oficial `adicionarCopia()` |
| `EmprestimoDAO` | salvar, `atualizarDevolucao`, `listarAtivosPorLeitor`, `buscarAtivoPorCopia` | Remonta as associações usando os outros DAOs |
| `ReservaDAO` | salvar, `atualizarSituacao`, `listarAtivasPorObra` | A lista ordenada por data é a fila da RN06 |

**Por que o `switch` do `LeitorDAO` não contradiz o polimorfismo?** Para **criar** um objeto é preciso escolher a classe concreta, porque não existe `new Leitor(...)`. Esse é o único ponto que conhece os tipos, como uma pequena fábrica; o resto do sistema usa polimorfismo.

**Dependência circular entre DAOs.** `ObraDAO` usa `CopiaDAO` e vice-versa. Se cada um criasse o outro como atributo, um construiria o outro infinitamente (`StackOverflowError`). Por isso o `CopiaDAO` cria o `ObraDAO` só dentro do método que precisa dele.

**Recursos novos do Java nos DAOs:**

- **Text blocks** (`"""`): textos de várias linhas, ótimos para SQL.
- **Switch expression** com `->`: devolve um valor e não precisa de `break`.
- **Operador ternário** `condição ? a : b`: um if/else de uma linha que devolve valor.
- **Varargs** (`Integer... parametros`): o método aceita qualquer quantidade de argumentos, recebidos como array.

**`salvar` × `atualizar`.** `salvar` sempre insere uma linha nova; nada é atualizado sozinho. Quem chama decide: se o objeto não tem id (`null`), é novo e vai para `salvar`; se tem id, veio do banco e vai para `atualizar`.

## Parte 5 – Exceção personalizada, senhas e services (passos 20 a 22)

Com esta parte, o back-end ficou completo: as regras RN01 a RN09 funcionam de ponta a ponta, do service ao banco, e foram verificadas por um cenário de 13 testes.

### Passo 20 – `NegocioException` e `SenhaUtil`

**`NegocioException`** (pacote `exception`) herda de `Exception`, então é **checked**: o compilador obriga quem chama a tratá-la ou repassá-la, e nenhuma tela pode “esquecer” de tratar uma regra violada. Ela representa uma regra quebrada (“a Ana tem livro atrasado”), enquanto a `SQLException` representa uma falha técnica. A tela trata as duas em `catch` separados.

A classe `Exception` já existe no Java (pacote `java.lang`, importado automaticamente). A hierarquia:

```
Object
└── Throwable
    ├── Error                 (falhas graves da JVM)
    └── Exception             (checked)
        ├── SQLException
        ├── NegocioException  (nossa)
        └── RuntimeException  (unchecked)
            ├── IllegalStateException
            └── NullPointerException
```

**`SenhaUtil`** (pacote `util`) implementa a decisão D07 com classes do próprio Java:

- `gerarSalt()`: 16 bytes de `SecureRandom` (o `Random` comum é previsível), convertidos em texto com Base64;
- `gerarHash(senha, salt)`: PBKDF2WithHmacSHA256 com 600.000 iterações e hash de 256 bits;
- `verificar(senha, salt, hash)`: recalcula o hash e compara com `MessageDigest.isEqual`, que leva o mesmo tempo independentemente de quantos caracteres batem.

**Hash não é criptografia.** Criptografia tem volta (com a chave, recupera-se o texto); hash não. Por isso o login recalcula e compara, e nem o sistema sabe a senha de ninguém. No teste, a mesma senha `123456` com dois salts gerou dois hashes completamente diferentes.

### Passos 21 e 22 – A camada service

O service **coordena**: consulta os DAOs, confere as regras, lança `NegocioException` quando uma é violada e, se tudo estiver certo, manda gravar. Ele não escreve SQL nem mostra nada na tela.

| Service | Responsabilidade | Regras |
| --- | --- | --- |
| `AutenticacaoService` | Login de funcionários | RN08 |
| `EmprestimoService` | Empréstimo e devolução | RN01 a RN04, RN06, RN07, RN09 |
| `ReservaService` | Reservar e cancelar | RN05, RN06 |
| `LeitorService` | Cadastro, duplicidade de CPF e matrícula, exclusão | – |
| `ObraService` | Cadastro de obras e cópias, busca de cópia por código | – |
| `FuncionarioService` | Cria admin / admin123 num banco vazio | – |

**A ordem das regras no empréstimo** segue a lógica de um bibliotecário:

1. a cópia pode sair? (disponível, ou reservada para este leitor, que é o primeiro da fila);
2. o leitor está abaixo do limite? (`leitor.getLimiteEmprestimos()`: polimorfismo, 3 ou 5);
3. o leitor tem algum empréstimo atrasado?
4. se tudo passou: grava o empréstimo, marca a cópia como EMPRESTADA e, se for o caso, atende a reserva.

**Na devolução:** o próprio `Emprestimo` calcula a multa; depois, se houver fila de reservas para a obra, a cópia fica RESERVADA; senão, volta a DISPONÍVEL.

**Pontos para estudar:**

- **`equals` em vez de `==` para `Integer`.** O `id` é um objeto; `==` compara se é o mesmo objeto na memória. O Java guarda em cache os `Integer` de -128 a 127, então `==` parece funcionar com ids pequenos e falha a partir do 128.
- **Uma tela, um método `salvar`.** O service decide entre `INSERT` e `UPDATE` pelo id (`null` = novo).
- **Tradução de erros.** Ao excluir um leitor com histórico, o banco recusa pela chave estrangeira; o service captura essa `SQLException` e lança uma `NegocioException` com mensagem clara.
- **Validação em duas camadas.** O model se protege com `IllegalStateException` (`reserva.cancelar()`), e o service confere antes para dar uma mensagem amigável.
- **Limitação consciente: transações.** Um empréstimo faz duas gravações, cada uma em sua conexão. Se o programa caísse entre elas, os dados ficariam inconsistentes. A solução profissional é uma transação (todas as gravações valem juntas ou nenhuma vale), deixada fora do escopo.

### Demonstração das regras pelo console

A classe `DemonstracaoRegras` recria o banco do zero e executa 13 cenários (login, atraso, limite, cópia indisponível, multa, fila de reservas, duplicidades, exclusão bloqueada). Serve de roteiro e de plano B para a apresentação:

```bash
mvn compile exec:java "-Dexec.mainClass=br.com.biblioteca.DemonstracaoRegras"
```

O método auxiliar `tentar(...)` faz o mesmo que cada botão das telas fará: um `catch` para `NegocioException` (regra) e outro para erros técnicos. As expressões `() -> ...` são **lambdas**, blocos de código passados como parâmetro.

## Parte 6 – Interface gráfica (passos 23 a 27)

A interface Swing tem seis telas (login, início, leitores, obras e cópias, empréstimos, reservas), com tema claro e escuro alternável e identidade em verde oliva. O layout foi gerado com auxílio de IA (decisão D08); o tratamento de exceções em cada botão foi revisado pelo autor.

### Passo 23 – Tema, login e janela principal

- **FlatLaf** (dependência `com.formdev:flatlaf` 3.7) substitui o visual padrão do Swing. A classe `Tema` define a cor de destaque (`@accentColor`), o arredondamento e alterna claro/escuro com `FlatLaf.updateUI()`.
- **`PainelMarca`** e **`CartaoIndicador`** estendem `JPanel` e sobrescrevem `paintComponent`, o método que o Swing chama para desenhar o componente. A estante de livros do login e os cartões do painel inicial são desenhados por código (`Graphics2D`, `fillRoundRect`).
- **`MainFrame`** usa `BorderLayout`: barra lateral no `WEST` e conteúdo no `CENTER`, com `CardLayout` (vários painéis empilhados, um visível por vez).
- **`App.main`** prepara o banco, garante o administrador e abre o login com `SwingUtilities.invokeLater`, porque toda criação e alteração de tela deve acontecer na thread do Swing (EDT).
- **`BorderLayout` e tamanhos:** `NORTH`/`SOUTH` respeitam a altura preferida; `CENTER` estica nas duas direções. Por isso os cartões foram colocados no `NORTH` de um painel intermediário.

### Passo 24 – Leitores e a classe abstrata `PainelTela`

Toda tela da barra lateral estende **`PainelTela`**, uma classe abstrata com o método abstrato `atualizar()` e um método herdado `criarCabecalho(...)`. A `MainFrame` guarda as telas num `Map<String, PainelTela>` e, ao exibir uma, chama `telas.get(nome).atualizar()` sem saber qual tela é: **polimorfismo na interface**.

```java
private void mostrar(String nome) {
    telas.get(nome).atualizar();   // cada tela atualiza do seu jeito
    cartoes.show(conteudo, nome);
}
```

Outros recursos da tela de leitores:

- **`SeloRenderer`** estende `DefaultTableCellRenderer` e sobrescreve `getTableCellRendererComponent` e `paintComponent` para desenhar “pílulas” coloridas. Recebe um `Map<String, Color>`, então serve a qualquer tabela.
- **`TableRowSorter` + `RowFilter`**: ordenação pelo cabeçalho e busca a cada letra. Com filtro ou ordenação, a linha visível difere da linha do modelo: usa-se `convertRowIndexToModel`.
- **Classe anônima** implementando `DocumentListener`: a interface é implementada ali mesmo, sem arquivo próprio.
- **Polimorfismo visível:** as colunas Prazo e Limite vêm de `getPrazoDias()` e `getLimiteEmprestimos()`. Ao trocar um leitor de Aluno para Professor, os valores mudam na tela.

### Passo 25 – Obras e cópias, e o `ModeloTabela<T>` genérico

Para não repetir um modelo de tabela para cada entidade, foi criada a classe abstrata genérica **`ModeloTabela<T>`**:

- **Generics:** `T` é o tipo dos itens; `ModeloTabela<Obra>` guarda obras, `ModeloTabela<Leitor>`, leitores.
- **Template Method:** a classe-mãe implementa `getRowCount`, `getColumnName` e `getValueAt`, que chama o método abstrato `valorDaColuna(item, coluna)`; cada subclasse só diz o que mostrar.
- **Refactoring:** o `LeitorTableModel` passou a estender `ModeloTabela<Leitor>` e ficou com poucas linhas, sem mudar o comportamento da tela.
- Os modelos de obras e de cópias são **classes anônimas** que estendem `ModeloTabela`.

A tela segue o padrão **mestre-detalhe**: ao selecionar uma obra, a tabela de baixo mostra as cópias que ela agrega, com selos de situação. É a **agregação visível na tela**.

### Passo 26 – Empréstimos

A tela registra empréstimos (leitor + código da cópia) e devoluções (código da cópia). O funcionário logado chega a ela pelo construtor, vindo da `MainFrame` (RN09).

- Ao escolher o leitor, a tela mostra “ALUNO · prazo de 7 dias · até 3 livros”: o polimorfismo aparece antes do empréstimo.
- A situação (ATIVO, ATRASADO, DEVOLVIDO) é calculada na tela a partir de `isDevolvido()` e `estaAtrasado(hoje)`.
- **Formatação é papel da view:** `NumberFormat` mostra `R$ 3,00` e `DateTimeFormatter` mostra `28/11/26`, enquanto o banco guarda `3.00` e `2026-11-28`.
- **`BigDecimal` e zero:** usa-se `signum()` ou `compareTo`. `new BigDecimal("0.00").equals(BigDecimal.ZERO)` é falso, porque o `equals` compara também as casas decimais.
- **`RowFilter.andFilter`** combina a busca por texto com o filtro de situação.

### Passo 27 – Reservas

A tabela mostra a posição de cada reserva ativa na fila da obra (RN06), e o formulário mostra quantas cópias da obra estão livres, explicando a RN05 antes do clique. A fila é calculada com:

- **`Comparator` com referências a métodos:** `Comparator.comparing(Reserva::getDataReserva).thenComparing(Reserva::getId)`;
- **`Map.merge`:** `contador.merge(obraId, 1, Integer::sum)` começa em 1 e soma 1 a cada nova reserva da mesma obra.

### O padrão de todo botão

Cada ação das telas segue o mesmo molde, que atende à exigência de tratar exceções perto da UI:

```java
private void emprestar() {
    try {
        // chama o service
    } catch (NegocioException e) {     // regra violada: aviso amarelo
        JOptionPane.showMessageDialog(this, e.getMessage(), "...", JOptionPane.WARNING_MESSAGE);
    } catch (SQLException e) {         // falha técnica: erro vermelho
        JOptionPane.showMessageDialog(this, "Erro ao acessar o banco...", "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
```

Na tela de obras há um terceiro `catch`, para `NumberFormatException`: uma exceção unchecked do Java, lançada quando o ano digitado não é número. O compilador não obriga a tratá-la, mas a tela trata por ser um erro previsível de digitação.

### Conceitos de POO presentes na interface

| Conceito | Onde |
| --- | --- |
| Classe abstrata | `PainelTela`, `ModeloTabela<T>` |
| Herança | Todas as telas, `SeloRenderer`, `PainelCartao`, `CartaoIndicador` |
| Sobrescrita | `paintComponent`, `getTableCellRendererComponent`, `valorDaColuna`, `atualizar` |
| Polimorfismo | `telas.get(nome).atualizar()`; `configurarBotaoBarra(AbstractButton)` aceita `JButton` e `JToggleButton` |
| Generics | `ModeloTabela<T>`, `JComboBox<Leitor>`, `TableRowSorter<...>` |
| Classe anônima | `DocumentListener` das buscas; modelos de obras, cópias, empréstimos e reservas |
| Lambda | `botao.addActionListener(e -> salvar())` |

## Teoria – Exceções

O enunciado pede que o tratamento de exceções fique perto da UI. No projeto, as exceções são **lançadas** onde o problema é detectado (model, DAO, service) e **tratadas** onde se sabe o que fazer com elas: a tela.

### Lançar × tratar

|  | Lançar (`throw`) | Tratar (`try/catch`) |
| --- | --- | --- |
| Onde | Onde o problema é detectado | Onde se sabe o que fazer |
| No projeto | `Reserva.atender()`, banco recusando `UNIQUE` | O `main` hoje; as telas Swing depois |
| Efeito | Interrompe o método e “joga” o erro para quem chamou | Captura o erro e mostra uma mensagem |

```java
// No model: detecta e lança
public void atender() {
    if (!isAtiva()) {
        throw new IllegalStateException("Só é possível atender uma reserva ativa.");
    }
    this.situacao = SituacaoReserva.ATENDIDA;
}

// Na "UI": captura e mostra
try {
    dao.salvar(ana);
} catch (SQLException e) {
    System.out.println("Erro no banco de dados: " + e.getMessage());
}
```

### Como a exceção sobe pelas camadas

Quando uma exceção é lançada, o Java abandona o método atual e volta pela pilha de chamadas até encontrar um `catch` compatível. Tudo o que vinha depois no bloco `try` **não é executado**. No teste de salvar duas vezes, a falha da Ana fez o programa pular direto para o `catch`, e o Carlos nem chegou a ser tentado.

| Situação | Alguém capturou? | Resultado |
| --- | --- | --- |
| `reserva.atender()` numa reserva cancelada | Não | `BUILD FAILURE` com stack trace |
| `UNIQUE` recusando matrícula repetida | Sim, o `catch` do `main` | `BUILD SUCCESS` com mensagem amigável |

### Checked × unchecked

- **Checked** (verificadas), como `SQLException`: o compilador obriga a tratar com `try/catch` ou declarar com `throws` na assinatura. Os DAOs declaram `throws SQLException` e repassam o erro.
- **Unchecked** (não verificadas), filhas de `RuntimeException`, como `IllegalStateException`, `NullPointerException` e `UnsupportedOperationException`: o compilador não obriga a tratar.

### Lendo um stack trace

```
java.lang.IllegalStateException: Só é possível atender uma reserva ativa.   ← o quê
    at br.com.biblioteca.model.Reserva.atender (Reserva.java:60)              ← onde nasceu
    at br.com.biblioteca.App.main (App.java:47)                               ← quem chamou
    at org.codehaus.mojo.exec...                                              ← Maven: ignorar
```

Regra prática: a primeira linha diz o tipo e a mensagem; depois, procure a primeira linha com `br.com.biblioteca`.

### Erro de compilação × erro de execução × erro de lógica

| Tipo | Quem detecta | Exemplo no projeto |
| --- | --- | --- |
| Compilação | O compilador, antes de rodar | `NULL` em vez de `null`; construtor duplicado |
| Execução (exceção) | A JVM, ao rodar | `add` na lista somente leitura |
| Lógica | Só os testes | `cancelar()` com a condição invertida; datas trocadas no construtor |

Compilar não significa funcionar: erros de lógica só aparecem comparando a saída com o valor esperado, calculado à mão.

## Registro de decisões (resumo)

O registro completo, com motivos detalhados, está em `docs/decisoes.md`, no formato de **ADR** (Architecture Decision Record): para cada escolha, a decisão, o motivo e o que foi descartado.

| Código | Decisão | Motivo principal |
| --- | --- | --- |
| D01 | Java 25 + Maven | Versão LTS instalada; Maven gerencia o driver do SQLite |
| D02 | SQLite via JDBC | Banco em arquivo, sem servidor |
| D03 | `Pessoa` e `Leitor` abstratas | Não existe pessoa nem leitor genérico |
| D04 | `BigDecimal` para a multa | `double` tem erros de arredondamento |
| D05 | Enums para situações | O compilador barra valores inválidos |
| D06 | Login de funcionários | Controle de acesso e rastreabilidade dos empréstimos |
| D07 | Senha com hash PBKDF2 e salt | Senha ilegível se o banco vazar; resistente a força bruta; sem dependência externa |
| D08 | Telas geradas com auxílio de IA | Autorizado pelo professor; foco no back-end e na POO |
| D09 | `id` como `Integer` | `null` representa “ainda não salvo” |
| D10 | Dois construtores por entidade | Objeto novo × lido do banco, sem repetir atribuições |
| D11 | `getTipo()` em Leitor | Grava Aluno e Professor na mesma tabela sem `instanceof` |
| D12 | Obra–Cópia bidirecional | Telas mostram o título a partir da cópia; `adicionarCopia()` mantém os dois lados |
| D13 | ConexaoFactory | Um só lugar para endereço e configuração do banco |
| D14 | Mapeamento da herança | Tabela por classe concreta + tabela única para leitores |
| D15 | Datas, dinheiro e enums como `TEXT` | Formato ISO ordena certo; evita imprecisão do `REAL` |
| D16 | Restrições no banco | Segunda linha de defesa da integridade |
| D17 | Criação automática das tabelas | Funciona em qualquer máquina sem configuração |

Decisões da Parte 5:

| Código | Decisão | Motivo principal |
| --- | --- | --- |
| D18 | `NegocioException` checked, separada de `SQLException` | O compilador obriga as telas a tratar regras violadas; regra e falha técnica têm tratamentos diferentes |
| D19 | Sem transações entre gravações | Simplicidade no escopo do projeto; risco documentado |
| D20 | Administrador criado automaticamente | Num banco novo, sempre há um login inicial (admin / admin123) |
| D21 | Service decide entre inserir e atualizar | A tela chama sempre `salvar`; o id `null` indica objeto novo |

Decisões da Parte 6:

| Código | Decisão | Motivo principal |
| --- | --- | --- |
| D22 | FlatLaf com tema claro/escuro e verde oliva | Visual moderno e próprio; troca de tema ao vivo |
| D23 | Telas herdam da classe abstrata `PainelTela` | A janela principal atualiza qualquer tela por polimorfismo |
| D24 | `ModeloTabela<T>` genérico (Template Method) | Elimina a repetição de um modelo de tabela por entidade |
| D25 | Componentes desenhados por código (selos, cartões, marca) | Identidade visual e exemplos de sobrescrita na interface |
| D26 | Formatação de datas e moeda só na view | O banco guarda formatos técnicos (ISO, `3.00`); a tela mostra o formato brasileiro |

## Erros comuns encontrados

Todos os erros abaixo aconteceram de verdade durante o projeto. O mais instrutivo foi o último: compilou, rodou e deu resultado errado, e só foi descoberto comparando a saída com o valor calculado à mão.

| Erro ou sintoma | Causa | Solução |
| --- | --- | --- |
| `there is no POM in this directory` | Maven executado fora da pasta do projeto | Rodar na pasta do `pom.xml` (terminal integrado do VSCode) |
| Pasta `.github/modernize` aparecendo sozinha | Extensão do VSCode criando arquivos | Adicionar ao `.gitignore`; conferir o que vai em cada commit |
| Pacote `Model` com maiúscula | Convenção: pacotes são minúsculos | Renomear para `model` |
| `constructor ... is already defined` | Dois construtores com os mesmos tipos de parâmetros | Diferenciar pela lista de tipos (o modificador e os nomes não contam) |
| `abstract methods cannot have a body` | Método abstrato escrito com chaves | Terminar em `;`, sem corpo |
| Constantes de Aluno escritas em Leitor | Confusão entre contrato e implementação | Valores ficam nas subclasses concretas |
| `statements not expected outside of methods` | Código solto na classe, fora de um método | Colocar os comandos dentro do `main` |
| `illegal start of expression` + `class ... expected` | Método colado dentro de outro; chave sobrando | Conferir o pareamento das chaves |
| `NULL` não reconhecido | Java diferencia maiúsculas; `NULL` é do C | Usar `null` |
| `incompatible types: String cannot be converted to SituacaoReserva` | `"ATIVA"` (texto) no lugar do enum | Usar `SituacaoReserva.ATIVA` |
| `UnsupportedOperationException` | `add` numa lista somente leitura | Comportamento esperado: usar `adicionarCopia()` |
| `cancelar()` recusando reservas ativas | Condição do `if` invertida (faltou o `!`) | `if (!isAtiva())` |
| `UNIQUE constraint failed: leitor.matricula` | Inserir o mesmo leitor duas vezes | Verificar se já existe antes de salvar |
| Data prevista igual à do empréstimo e multa de R$ 20 em vez de R$ 13 | Dois `LocalDate` em ordem diferente no construtor e no DAO; o compilador não detecta | Ordem cronológica igual em atributos, construtores e colunas |

**Lição geral:** `BUILD SUCCESS` não prova que o programa está certo. Todo teste deve ter um resultado esperado calculado antes, para ser comparado com a saída.

Erros encontrados na construção dos services e das telas:

| Erro ou sintoma | Causa | Solução |
| --- | --- | --- |
| VSCode sublinha erros, mas `mvn compile` passa | Cache desatualizado do Java Language Server (após mudar o `pom.xml`) | “Reload Projects” no `pom.xml` ou “Java: Clean Java Language Server Workspace”. O Maven é a referência |
| Cartões do início esticados até o fim da tela | Componente no `CENTER` do `BorderLayout` estica nas duas direções | Colocar a grade no `NORTH` de um painel intermediário |
| Selo “PROFESSOR” e CPF cortados | Colunas demais para a largura disponível | Tirar o e-mail da tabela (já está no formulário) e ajustar larguras |
| Datas cortadas (“05/12/202…”) | Formato longo para a coluna | Formato `dd/MM/yy` |
| Duas obras sem ISBN seriam recusadas | `""` conta como valor repetido no `UNIQUE`; `NULL` não | O service converte ISBN em branco para `null` |

| Erro ou sintoma | Causa | Solução |
| --- | --- | --- |
| `-Dexec.mainClass=...` ignorado; o Maven sempre roda o `App` | `<mainClass>` fixo na configuração do plugin tem prioridade sobre a linha de comando | Definir `<exec.mainClass>` em `<properties>` e remover a configuração do plugin; o `-D` passa a sobrescrever |

## Roteiro da apresentação

A apresentação tem três partes: o programa rodando, um passeio pelo código seguindo um empréstimo da tela até o banco, e as perguntas do professor.

### Antes de começar

1. Rodar a preparação dos dados (as datas são relativas ao dia, então sempre haverá um atraso, uma multa e uma reserva):

```bash
mvn compile exec:java "-Dexec.mainClass=br.com.biblioteca.PreparacaoApresentacao"
```

2. Abrir no VSCode, nesta ordem, as abas do passeio pelo código: `EmprestimosPanel`, `EmprestimoService`, `Emprestimo`, `Leitor`, `Aluno`, `EmprestimoDAO`, `Obra`.
3. Aumentar a fonte do editor (`Ctrl` + `=`) para o professor ler da tela.
4. Plano B, se a interface falhar: `DemonstracaoRegras` mostra as regras pelo console (lembrando que ela apaga o banco).

### Parte 1 – O programa rodando (cerca de 5 minutos)

| Tela | O que mostrar | Regra ou conceito |
| --- | --- | --- |
| Login | Senha errada → aviso; `admin` / `admin123` → entra | RN08; senha com hash |
| Início | Cartões com 1 atraso em vermelho | Visão geral |
| Leitores | Prazo 7/14 e limite 3/5; trocar Bruno para Professor e voltar | Herança e polimorfismo |
| Obras | *Dom Casmurro* com as cópias emprestadas | Agregação |
| Empréstimos | Ana bloqueada pelo atraso; Bruno empresta a C003 | RN04, RN01 |
| Empréstimos | Devolução da C001: multa de R$ 5,00 e cópia RESERVADA | RN07, RN06 |
| Empréstimos | Bruno recusado na C001; Diego empresta | RN06, RN02 |
| Reservas | Reserva do Diego ATENDIDA; reservar *O Cortiço* → recusado | RN05 |
| Qualquer tela | Alternar o tema | Interface |

### Parte 2 – O código: um empréstimo da tela ao banco (cerca de 3 minutos)

Seguir o caminho de um único empréstimo cobre quase todos os conceitos pedidos no enunciado:

| # | Arquivo e trecho | O que dizer |
| --- | --- | --- |
| 1 | `EmprestimosPanel.emprestar()` | “O botão chama o service dentro de um `try`. Os `catch` ficam aqui, na UI: regra violada vira aviso, falha de banco vira erro.” |
| 2 | `EmprestimoService.realizarEmprestimo()` | “O service aplica as regras na ordem de um bibliotecário e lança `NegocioException`. O limite vem de `leitor.getLimiteEmprestimos()`, sem saber se é aluno ou professor.” |
| 3 | Construtor de `Emprestimo` | “A data prevista é `dataEmprestimo.plusDays(leitor.getPrazoDias())`: polimorfismo.” |
| 4 | `Leitor` e `Aluno` | “`Leitor` é abstrata e herda de `Pessoa`; declara os métodos abstratos. `Aluno` sobrescreve com 7 e 3.” |
| 5 | `EmprestimoDAO.salvar()` | “O DAO grava com `PreparedStatement`. As associações com Leitor, Cópia e Funcionário viram chaves estrangeiras.” |
| 6 | `Obra.adicionarCopia()` e `getCopias()` | “Agregação com encapsulamento: a lista é somente leitura e só muda por este método.” |

Atalhos do VSCode para achar tudo rápido: `Ctrl` + `P` abre um arquivo pelo nome; `Ctrl` + `Shift` + `O` lista os métodos do arquivo aberto; `F12` vai para a definição do que está sob o cursor.

### Parte 3 – Mapa para as perguntas

| Se o professor perguntar sobre… | Abrir | Mostrar |
| --- | --- | --- |
| Herança | `Leitor.java` | `extends Pessoa` e o `super(...)` nos construtores |
| Classe abstrata | `Pessoa.java`, `Leitor.java` | `abstract class` e os métodos abstratos terminando em `;` |
| Polimorfismo | `Aluno.java`, `Professor.java` | `@Override getPrazoDias()`; depois o construtor de `Emprestimo` |
| Polimorfismo na interface | `MainFrame.mostrar()` | `telas.get(nome).atualizar()` |
| Encapsulamento | `Obra.java` | `getCopias()` com `unmodifiableList`; ausência de `setCopias` |
| Agregação | `Obra.adicionarCopia()` | Os dois lados ligados; tela de obras como mestre-detalhe |
| Associação | `Emprestimo.java` | Atributos `leitor`, `copia`, `funcionario` |
| Sobrecarga | `Pessoa.java` | Os dois construtores e o `this(null, ...)` |
| Camada DAO | `LeitorDAO.java` | `salvar` com `PreparedStatement`; `montarLeitor` com o `switch` |
| Conexão com o banco | `ConexaoFactory.java` | URL do SQLite e `PRAGMA foreign_keys` |
| Herança no banco | `InicializadorBanco.java` | Tabela `leitor` com a coluna `tipo` |
| Exceções | `NegocioException.java`, `EmprestimosPanel.emprestar()` | `extends Exception` (checked) e os `catch` na tela |
| Senhas | `SenhaUtil.java` | `gerarHash` com salt e PBKDF2; `verificar` recalcula e compara |
| Generics | `ModeloTabela.java` | `<T>` e o método abstrato `valorDaColuna` |
| Regras de negócio | `EmprestimoService.java` | A sequência de verificações antes de gravar |

### Se algo der errado ao vivo

- **Mensagem de erro inesperada:** ler a mensagem em voz alta e explicar a camada de onde ela veio. Mostrar que o erro foi tratado, e não derrubou o programa, também demonstra o controle de exceções.
- **Dados bagunçados após várias demonstrações:** fechar o sistema e rodar a preparação de novo.
- **Interface não abre:** usar a `DemonstracaoRegras` pelo console.

## Perguntas prováveis do professor

As respostas abaixo são curtas de propósito: dê a resposta e aponte o lugar do código onde ela aparece.

**1. Onde está a herança no projeto?** Em `Pessoa` → `Leitor` e `Funcionario`, e em `Leitor` → `Aluno` e `Professor`. A relação é “é um”: um Aluno é um Leitor, que é uma Pessoa. Os dados comuns ficam na mãe e são herdados.

**2. Onde está o polimorfismo?** Em `getPrazoDias()`, `getLimiteEmprestimos()` e `getTipo()`: declarados abstratos em `Leitor` e sobrescritos em `Aluno` e `Professor`. O construtor do `Emprestimo` chama `leitor.getPrazoDias()` sem saber o tipo, e o Java escolhe a versão em tempo de execução.

**3. Qual a diferença entre agregação e composição? Qual você usou?** Nas duas há relação todo–parte. Na composição, a parte não existe sem o todo; na agregação, existe de forma independente. Usei agregação entre Obra e Cópia: a cópia é criada fora e agregada com `adicionarCopia()`.

**4. E a associação?** Empréstimo conhece Leitor, Cópia e Funcionário; Reserva conhece Leitor e Obra. Cada objeto existe sozinho. No banco, cada associação é uma chave estrangeira.

**5. Por que `Leitor` é abstrata?** Porque não existe leitor genérico: todo leitor é Aluno ou Professor. Ela define o contrato (métodos abstratos) e as subclasses o cumprem. O compilador obriga cada subclasse concreta a implementar os métodos abstratos.

**6. Por que não usar `if (leitor instanceof Aluno)`?** Porque cada novo tipo de leitor obrigaria a mudar todos esses `if`. Com polimorfismo, basta criar uma subclasse (princípio aberto/fechado). O único ponto que distingue os tipos é o `montarLeitor` do DAO, porque para criar um objeto é preciso escolher a classe concreta.

**7. O que é encapsulamento e onde você aplicou?** É proteger o estado do objeto com atributos `private` e acesso por métodos. O exemplo mais forte é a Obra: `getCopias()` devolve uma lista somente leitura, e só `adicionarCopia()` inclui cópias, mantendo os dois lados da relação consistentes.

**8. Diferença entre sobrecarga e sobrescrita?** Sobrecarga: mesmo nome, parâmetros diferentes, na mesma classe (os dois construtores). Sobrescrita: mesma assinatura, na subclasse (`getPrazoDias()` em Aluno). A primeira é resolvida na compilação; a segunda, na execução.

**9. O que é a camada DAO e por que usá-la?** É a camada que isola o acesso ao banco: traduz objetos em linhas e linhas em objetos. O resto do sistema não vê SQL. Se o banco mudar, só os DAOs e a `ConexaoFactory` mudam.

**10. Onde as exceções são tratadas?** São lançadas onde o problema é detectado (model, DAO, service) e capturadas na camada de UI, que mostra a mensagem ao usuário. Os DAOs declaram `throws SQLException` e não tratam nada.

**11. Por que `BigDecimal` e não `double` na multa?** `double` tem imprecisão binária (0.1 + 0.2 ≠ 0.3). Com dinheiro isso é inaceitável. Por isso também gravamos a multa como texto no banco, e não como `REAL`.

**12. Como a herança foi para o banco?** `Pessoa` não tem tabela (tabela por classe concreta). Aluno e Professor dividem a tabela `leitor` com a coluna `tipo` (tabela única), preenchida por `getTipo()`.

**13. O que é SQL Injection e como você evitou?** É quando um texto digitado pelo usuário altera o comando SQL. Evitei usando `PreparedStatement` com `?`: os valores são sempre tratados como dados.

**14. Como a senha é guardada?** Nunca em texto puro. O banco guarda um salt aleatório por funcionário e o hash PBKDF2 da senha. No login, a senha digitada passa pelo mesmo cálculo e os hashes são comparados.

**15. Por que o `id` é `Integer` e não `int`?** Para representar “ainda não salvo” com `null`. Com `int` seria `0`, que parece um id válido. O DAO preenche o id depois do `INSERT`.

**16. O que a camada service faz que o DAO não faz?** O DAO só grava e lê. O service aplica as regras: confere limite, atraso, disponibilidade e fila de reservas, coordena vários DAOs numa operação e lança `NegocioException` com mensagem para o usuário.

**17. Por que a `NegocioException` é checked?** Para o compilador obrigar a tela a tratá-la. Assim nenhuma regra violada derruba o programa por falta de `try/catch`, o que atende à exigência de controle de exceções perto da UI.

**18. Onde está o polimorfismo na interface?** Na `MainFrame`: as telas são guardadas como `PainelTela` (classe abstrata) e, ao trocar de tela, chama-se `atualizar()` sem saber qual tela é. Também no `ModeloTabela<T>`, em que cada tabela implementa `valorDaColuna` do seu jeito.

**19. O que é o `ModeloTabela<T>`?** Uma classe abstrata genérica que adapta uma lista de objetos para a `JTable`. Ela segue o padrão Template Method: implementa a parte comum e deixa para as subclasses só o conteúdo de cada coluna.

**20. Por que a tela não acessa o DAO diretamente?** Para respeitar as camadas: a tela chama o service, que aplica as regras. Se a tela gravasse direto pelo DAO, poderia, por exemplo, emprestar uma cópia indisponível sem nenhuma verificação.

## Glossário

| Termo | Significado |
| --- | --- |
| `abstract` | Classe que não pode ser instanciada, ou método sem corpo que as subclasses devem implementar |
| ADR | Architecture Decision Record: registro de uma decisão, seu motivo e as alternativas descartadas |
| Agregação | Relação todo–parte em que a parte existe independente do todo |
| Associação | Um objeto conhece e usa outro; ambos têm vida própria |
| Chave estrangeira | Coluna que aponta para o id de outra tabela (`copia.obra_id`) |
| Composição | Relação todo–parte em que a parte não existe sem o todo |
| Construtor | Método especial, com o nome da classe e sem tipo de retorno, que inicializa o objeto |
| DAO | Data Access Object: classe que isola o acesso ao banco |
| Encadeamento de construtores | Um construtor chamar outro com `this(...)` ou `super(...)` |
| Enum | Tipo com conjunto fixo de valores |
| Exceção checked | Exceção que o compilador obriga a tratar ou declarar (`SQLException`) |
| Exceção unchecked | Filha de `RuntimeException`; tratamento não obrigatório |
| Factory | Padrão em que uma classe centraliza a criação de objetos (`ConexaoFactory`) |
| `final` | Em atributo: não muda de valor; em classe: não pode ser herdada |
| Guard clause | Tratar o caso especial no início do método e sair cedo |
| Hash | Função de mão única: da senha chega-se ao hash, mas não o contrário |
| JDBC | API padrão do Java para bancos relacionais |
| `@Override` | Anotação que pede ao compilador para confirmar uma sobrescrita |
| Pacote | Agrupamento de classes; corresponde a uma pasta |
| `PreparedStatement` | Comando SQL com `?`, preenchidos com segurança |
| `ResultSet` | Resultado de uma consulta, lido linha por linha |
| Salt | Valor aleatório misturado à senha antes do hash |
| Sobrecarga | Mesmo nome de método, parâmetros diferentes |
| Sobrescrita | Subclasse reimplementa um método herdado, com a mesma assinatura |
| SQL Injection | Ataque em que texto do usuário altera um comando SQL |
| Stack trace | Lista das chamadas ativas quando uma exceção ocorreu |
| `static` | Pertence à classe, não a cada objeto |
| `super` | Referência à classe-mãe; `super(...)` chama o construtor dela |
| `this` | Referência ao próprio objeto; `this(...)` chama outro construtor da mesma classe |
| Try-with-resources | `try (recurso) { }` que fecha o recurso automaticamente |

Termos da interface gráfica:

| Termo | Significado |
| --- | --- |
| `BorderLayout` | Organiza em cinco regiões: NORTH, SOUTH, EAST, WEST e CENTER |
| `CardLayout` | Empilha painéis e mostra um de cada vez |
| Classe anônima | Classe sem nome, criada e usada no mesmo ponto do código (`new DocumentListener() { ... }`) |
| EDT | Event Dispatch Thread: a thread em que o Swing cria e atualiza as telas |
| Generics | Tipos parametrizados (`ModeloTabela<T>`), que funcionam para vários tipos com segurança |
| Lambda | Bloco de código passado como valor (`e -> salvar()`) |
| Look and Feel | A “pele” visual de todos os componentes Swing (aqui, FlatLaf) |
| `paintComponent` | Método que o Swing chama para desenhar um componente; sobrescrito para desenhos próprios |
| Renderer | Objeto que desenha cada célula de uma tabela (`SeloRenderer`) |
| `TableModel` | Adapta os dados para a `JTable`: quantas linhas, colunas e o valor de cada célula |
| Template Method | Padrão em que a classe-mãe define o algoritmo e as filhas implementam só os passos variáveis |
