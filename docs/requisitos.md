# Requisitos – Sistema de Biblioteca

## Entidades
- **Pessoa** (abstrata): nome, CPF, e-mail, telefone
- **Leitor** (é uma Pessoa): matrícula
  - **Aluno** e **Professor** (tipos de leitor com regras diferentes)
- **Funcionário** (é uma Pessoa): cargo, login, senha
- **Obra**: título, autor, editora, ano, ISBN
- **Cópia**: código do exemplar, situação (DISPONÍVEL, EMPRESTADA, RESERVADA, MANUTENÇÃO)
- **Empréstimo**: leitor, cópia, funcionário, data do empréstimo, data prevista, data da devolução
- **Reserva**: leitor, obra, data da reserva, situação (ATIVA, ATENDIDA, CANCELADA)

## Regras de negócio
- RN01: Aluno pode ter até 3 empréstimos ativos, prazo de 7 dias.
- RN02: Professor pode ter até 5 empréstimos ativos, prazo de 14 dias.
- RN03: Só pode ser emprestada uma cópia com situação DISPONÍVEL.
- RN04: Leitor com empréstimo em atraso não pode fazer novo empréstimo.
- RN05: Só é possível reservar uma obra que não tenha cópia disponível.
- RN06: Ao devolver uma cópia de obra com reserva ativa, a cópia fica RESERVADA
  para o leitor da reserva mais antiga.
- RN07: Cada dia de atraso na entrega gera uma multa de 1 real.
- RN08: Só funcionários autenticados (login e senha) acessam o sistema.
- RN09: O empréstimo é registrado em nome do funcionário que está logado. 
