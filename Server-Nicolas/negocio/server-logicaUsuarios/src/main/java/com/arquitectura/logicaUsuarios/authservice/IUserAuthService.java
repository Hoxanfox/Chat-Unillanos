package com.arquitectura.logicaUsuarios.authservice;

import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;

public interface IUserAuthService {
    void registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress) throws Exception;
    UserResponseDto autenticarUsuario(LoginRequestDto requestDto, String ipAddress) throws Exception;
}