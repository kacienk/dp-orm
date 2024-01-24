package orm.tableInheritance

class RelationsHandler {
    // when mapper stumbles upon a relation:
    // invoked by some mapper's find method
    // it resolves relation by calling appropiate new mapper
    // for the entity which is on the other side of the relation
    // and invoking findWithoutRelations on this new entity
}