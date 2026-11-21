# Registro de Decisões

## D01 – Java 25 + Maven
**Decisão:** usar JDK 25 (LTS) e Maven para build.
**Motivo:** versão LTS já instalada; Maven gerencia o driver do SQLite.

## D02 – SQLite
**Decisão:** banco SQLite via JDBC (sqlite-jdbc).
**Motivo:** banco em arquivo, sem servidor; ideal para aprendizado.

## D03 – Pessoa e Leitor abstratas
**Decisão:** Pessoa e Leitor são classes abstratas
**Motivo:** Não existem pessoa genérica, nem leitor genérico. Uma pessoa necessariamente será um Leitor ou um Funcionário. E um Leitor necessariamente será um Aluno ou um Professor.

## D04 – BigDecimal para multa
**Decisão:** Usamos BigDecimal para multa em vez de double
**Motivo:** double pode ocasionar erros de arredondamento, que não podem ocorrer quando se trata de valor monetário. 

## D05 - ENUM para situações
**Decisão:** Situações/Status viraram ENUM. 
**Motivo:** Se torna impossível escrever o status errado e salvar no sistema, o compilador barra. 

## D06 – Login de funcionários
**Decisão:** o sistema exige login e senha de um funcionário para ser acessado, e o funcionário logado é registrado automaticamente nos empréstimos (RN08 e RN09).
**Motivo:** controla quem usa o sistema e garante rastreabilidade: cada empréstimo fica associado a quem o realizou, sem depender de o usuário escolher o nome numa lista.

## D07 – Senha armazenada com hash PBKDF2 e salt
**Decisão:** a senha nunca é salva em texto puro. O banco guarda apenas o salt (aleatório, um por funcionário) e o hash gerado com PBKDF2WithHmacSHA256. No login, a senha digitada passa pelo mesmo cálculo e os hashes são comparados.
**Motivo:** se o arquivo do banco vazar, as senhas não podem ser lidas. O salt impede que senhas iguais gerem hashes iguais, e o PBKDF2, por ser lento de propósito, dificulta ataques de força bruta. Foi escolhido em vez de SHA-256 simples (rápido demais para senhas) e de bibliotecas externas como BCrypt, porque já faz parte do Java e não exige dependência nova.

## D08 – Interface gráfica gerada com auxílio de IA
**Decisão:** o layout das telas Swing foi gerado com auxílio de IA. O tratamento de eventos e de exceções na camada de UI foi implementado e revisado pelo autor.
**Motivo:** o foco do projeto é o back-end e os conceitos de POO; a geração do layout liberou tempo para essas partes.

## D09 – Id como Integer
**Decisão:** o atributo `id` das entidades é `Integer`, e não `int`.
**Motivo:** um objeto recém-criado ainda não foi salvo no banco e, portanto, não tem id. Com `Integer`, esse estado é representado por `null`; com `int`, seria `0`, um valor que parece válido.

## D10 – Dois construtores por entidade
**Decisão:** cada entidade tem um construtor sem `id` (objeto novo) e outro com `id` (objeto lido do banco). O primeiro chama o segundo, passando `null` no lugar do id.
**Motivo:** evita passar `null` manualmente ao cadastrar e mantém as atribuições escritas em um único lugar (encadeamento de construtores com `this(...)` e `super(...)`).

## D11 – Método getTipo() em Leitor
**Decisão:** `Leitor` declara o método abstrato `getTipo()`, implementado por Aluno ("ALUNO") e Professor ("PROFESSOR").
**Motivo:** permite gravar os dois tipos na mesma tabela do banco, com uma coluna `tipo`, sem usar `instanceof`. Novos tipos de leitor não exigem mudanças no código que já existe.

## D12 – Relação bidirecional entre Obra e Copia
**Decisão:** a Obra mantém a lista de suas cópias e cada Copia guarda uma referência à sua Obra. A lista é exposta apenas como somente leitura e alterada só por `adicionarCopia()`.
**Motivo:** as telas precisam exibir o título da obra a partir de uma cópia, e o DAO precisa saber a qual obra cada cópia pertence. Centralizar a inclusão em `adicionarCopia()` garante que os dois lados da relação fiquem sempre consistentes.
