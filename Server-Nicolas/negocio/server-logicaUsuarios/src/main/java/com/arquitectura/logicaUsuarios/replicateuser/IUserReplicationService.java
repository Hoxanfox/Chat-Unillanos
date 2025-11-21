package com.arquitectura.logicaUsuarios.replicateuser;

import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;

public interface IUserReplicationService {
    /**
     * Recibe los datos de un usuario de otro servidor y lo guarda localmente.
     * No dispara correos de bienvenida ni validaciones de UI.
     */
    void guardarUsuarioReplicado(UserRegistrationRequestDto userDto) throws Exception;
}