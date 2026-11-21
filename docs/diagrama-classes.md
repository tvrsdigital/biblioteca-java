# Diagrama de Classes

```mermaid
classDiagram
    class Pessoa {
        <<abstract>>
        -Integer id
        -String nome
        -String cpf
        -String email
        -String telefone
        +toString() String
    }
    class Leitor {
        <<abstract>>
        -String matricula
        +getPrazoDias()* int
        +getLimiteEmprestimos()* int
        +getTipo()* String
    }
    class Aluno {
        -int PRAZO_DIAS = 7$
        -int LIMITE_EMPRESTIMOS = 3$
        +getPrazoDias() int
        +getLimiteEmprestimos() int
        +getTipo() String
    }
    class Professor {
        -int PRAZO_DIAS = 14$
        -int LIMITE_EMPRESTIMOS = 5$
        +getPrazoDias() int
        +getLimiteEmprestimos() int
        +getTipo() String
    }
    class Funcionario {
        -String cargo
        -String login
        -String senhaHash
        -String salt
    }
    class Obra {
        -Integer id
        -String titulo
        -String autor
        -String editora
        -int ano
        -String isbn
        -List~Copia~ copias
        +getCopias() List~Copia~
        +adicionarCopia(Copia copia) void
        +temCopiaDisponivel() boolean
        +toString() String
    }
    class Copia {
        -Integer id
        -String codigo
        -SituacaoCopia situacao
        -Obra obra
        +isDisponivel() boolean
        +toString() String
    }
    class SituacaoCopia {
        <<enumeration>>
        DISPONIVEL
        EMPRESTADA
        RESERVADA
        MANUTENCAO
    }
    class Emprestimo {
        +BigDecimal MULTA_POR_DIA = 1.00$
        -Integer id
        -LocalDate dataEmprestimo
        -LocalDate dataPrevista
        -LocalDate dataDevolucao
        -BigDecimal multa
        +isDevolvido() boolean
        +estaAtrasado(LocalDate hoje) boolean
        +calcularMulta(LocalDate data) BigDecimal
        +registrarDevolucao(LocalDate data) void
    }
    class Reserva {
        -Integer id
        -LocalDate dataReserva
        -SituacaoReserva situacao
        +isAtiva() boolean
        +atender() void
        +cancelar() void
    }
    class SituacaoReserva {
        <<enumeration>>
        ATIVA
        ATENDIDA
        CANCELADA
    }

    Pessoa <|-- Leitor
    Pessoa <|-- Funcionario
    Leitor <|-- Aluno
    Leitor <|-- Professor
    Obra "1" o-- "0..*" Copia : possui
    Copia "0..*" --> "1" Obra : pertence a
    Copia --> SituacaoCopia
    Reserva --> SituacaoReserva
    Emprestimo "0..*" --> "1" Leitor
    Emprestimo "0..*" --> "1" Copia
    Emprestimo "0..*" --> "1" Funcionario : registrado por
    Reserva "0..*" --> "1" Leitor
    Reserva "0..*" --> "1" Obra
```

## Legenda

| Símbolo | Conceito | Onde aparece |
|---|---|---|
| `<\|--` | Herança | Pessoa → Leitor/Funcionario; Leitor → Aluno/Professor |
| método com `*` | Método abstrato (base do polimorfismo) | `getPrazoDias()`, `getLimiteEmprestimos()`, `getTipo()` em Leitor |
| `o--` | Agregação | Obra ◇— Copia |
| `-->` | Associação | Emprestimo e Reserva com Leitor, Copia, Obra e Funcionario; Copia → Obra |
| `$` | Membro estático (constante) | `PRAZO_DIAS`, `LIMITE_EMPRESTIMOS` |

## Observações

- `Pessoa` e `Leitor` são abstratas: só Aluno, Professor e Funcionario são instanciados.
- O `id` é `Integer` para que `null` represente um objeto ainda não salvo no banco.
- `getTipo()` permite gravar Aluno e Professor na mesma tabela, com uma coluna `tipo`, sem uso de `instanceof`.
- A relação Obra–Copia é bidirecional: a Obra mantém a lista de cópias e cada Copia conhece sua Obra. A lista só é alterada por `adicionarCopia()`, que mantém os dois lados consistentes.
- `Funcionario` guarda apenas o hash e o salt da senha, nunca a senha em texto puro.
- `Emprestimo` calcula a data prevista com `leitor.getPrazoDias()` (polimorfismo) e recebe as datas como parâmetro nos métodos, o que permite testar atrasos sem depender do relógio.
- `Emprestimo` e `Reserva` quase não têm setters: seu estado muda apenas por métodos com significado (`registrarDevolucao`, `atender`, `cancelar`). `Reserva` lança `IllegalStateException` se a operação não for permitida na situação atual.