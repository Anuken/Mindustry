
#ifndef LENGTH
#error Please define a LENGTH
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform vec2 SampleOffsets[LENGTH];
uniform float SampleWeights[LENGTH];

void main()
{
	vec4 c = vec4(0);

	// Combine a number of weighted image filter taps.
	for (int i = 0; i < LENGTH; i++)
	{
		c += texture2D(u_texture0, v_texCoords + SampleOffsets[i]) * SampleWeights[i];
	}

	gl_FragColor = c;
}